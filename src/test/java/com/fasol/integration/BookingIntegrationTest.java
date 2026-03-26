package com.fasol.integration;

import com.fasol.domain.entity.Course;
import com.fasol.domain.entity.Schedule;
import com.fasol.domain.entity.StudentCourse;
import com.fasol.domain.entity.Teacher;
import com.fasol.domain.entity.User;
import com.fasol.domain.enums.AppRole;
import com.fasol.domain.enums.LessonType;
import com.fasol.dto.request.BookingRequest;
import com.fasol.exception.SlotFullException;
import com.fasol.repository.BookingRepository;
import com.fasol.repository.CourseRepository;
import com.fasol.repository.ScheduleRepository;
import com.fasol.repository.StudentCourseRepository;
import com.fasol.repository.TeacherRepository;
import com.fasol.repository.UserRepository;
import com.fasol.service.BookingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fasol_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private StudentCourseRepository studentCourseRepository;
    @Autowired private BookingRepository bookingRepository;

    private Course course;
    private Schedule groupSchedule;
    private List<User> students;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        studentCourseRepository.deleteAll();
        scheduleRepository.deleteAll();
        courseRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();

        course = courseRepository.save(Course.builder()
                .name("Вокал — базовый")
                .price(BigDecimal.valueOf(300))
                .individualLessons(4)
                .groupLessons(8)
                .build());

        User teacherUser = userRepository.save(User.builder()
                .email("teacher@fasol.by")
                .passwordHash("hash")
                .firstName("Анна")
                .lastName("Иванова")
                .roles(Set.of(AppRole.TEACHER))
                .build());

        Teacher teacher = teacherRepository.save(Teacher.builder()
                .user(teacherUser)
                .specialization("Вокал")
                .build());

        groupSchedule = scheduleRepository.save(Schedule.builder()
                .teacher(teacher)
                .course(course)
                .dayOfWeek(1)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .lessonType(LessonType.GROUP)
                .maxParticipants(3)
                .build());

        students = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User student = userRepository.save(User.builder()
                    .email("student" + i + "@test.com")
                    .passwordHash("hash")
                    .firstName("Студент")
                    .lastName(String.valueOf(i))
                    .roles(Set.of(AppRole.STUDENT))
                    .build());

            studentCourseRepository.save(StudentCourse.builder()
                    .student(student)
                    .course(course)
                    .individualLessonsRemaining(4)
                    .groupLessonsRemaining(4)
                    .build());

            students.add(student);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Race condition: 5 concurrent bookings for 3-person group → exactly 3 succeed")
    void concurrentBookings_exactlyThreeSucceed() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(5);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Exception> unexpected = Collections.synchronizedList(new ArrayList<>());

        LocalDate date = LocalDate.now().plusDays(3);

        for (int i = 0; i < 5; i++) {
            final UUID studentId = students.get(i).getId();
            pool.submit(() -> {
                try {
                    start.await();
                    bookingService.createBooking(studentId,
                            BookingRequest.builder()
                                    .scheduleId(groupSchedule.getId())
                                    .bookingDate(date)
                                    .build());
                    success.incrementAndGet();
                } catch (SlotFullException e) {
                    rejected.incrementAndGet();
                } catch (Exception e) {
                    unexpected.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(unexpected).as("No unexpected errors").isEmpty();
        assertThat(success.get()).as("Exactly 3 bookings").isEqualTo(3);
        assertThat(rejected.get()).as("Exactly 2 rejections").isEqualTo(2);

        long dbCount = bookingRepository.countConfirmedByScheduleAndDate(
                groupSchedule.getId(), date);
        assertThat(dbCount).isEqualTo(3);
    }

    @Test
    @Order(2)
    @DisplayName("Cancel before 24h deadline → balance restored")
    void cancelBooking_restoresBalance() {
        User student = students.get(0);
        LocalDate date = LocalDate.now().plusDays(3);

        var response = bookingService.createBooking(student.getId(),
                BookingRequest.builder()
                        .scheduleId(groupSchedule.getId())
                        .bookingDate(date)
                        .build());

        var scAfterBook = studentCourseRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow();
        assertThat(scAfterBook.getGroupLessonsRemaining()).isEqualTo(3);

        bookingService.cancelBooking(UUID.fromString(response.getId()), student.getId());

        var scAfterCancel = studentCourseRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow();
        assertThat(scAfterCancel.getGroupLessonsRemaining()).isEqualTo(4);
    }
}
