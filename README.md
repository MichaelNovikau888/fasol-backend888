# Fa Sol School — Java Backend

Spring Boot монолит для школы вокала «Фа Соль».
Переписан с Supabase/React на Java — с решением реальных проблем оригинала.

## Стек
- **Java 17** + Spring Boot 3.2
- **PostgreSQL 16** — основное хранилище
- **Redis** (Redisson) — distributed lock для race condition
- **Apache Kafka** — event-driven уведомления, Outbox pattern
- **Flyway** — миграции БД
- **Prometheus + Grafana** — метрики и алертинг
- **Testcontainers** — интеграционные тесты с реальной инфраструктурой

## Ключевые архитектурные решения

### 1. Race condition при групповом бронировании
**Проблема оригинала (Booking.tsx):** JS считал свободные места на уже загруженных данных.  
При двух одновременных запросах оба видели `availableSpots=1` и оба записывались.

**Решение:**
- Redis distributed lock (5s TTL) — блокирует слот на время транзакции
- `@Version` optimistic lock на `StudentCourse` — второй барьер для баланса
- `@Transactional` — атомарность INSERT + UPDATE в одной операции

### 2. Потеря занятий при отмене
**Проблема оригинала (Dashboard.tsx handleCancelBooking):**  
Только `UPDATE status='cancelled'` — баланс занятий НЕ возвращался.

**Решение:** compensation transaction — `incrementBalance()` в том же `@Transactional`.

### 3. Потеря Kafka-событий
**Проблема:** если Kafka недоступна в момент бронирования — событие теряется.

**Решение:** Transactional Outbox Pattern — событие сохраняется в `outbox_events`
в той же транзакции что и бронирование. Scheduler публикует асинхронно.

### 4. Нет проверки 24ч дедлайна на бэкенде
**Проблема оригинала:** проверка только на фронтенде — обходится прямым API-запросом.

**Решение:** `BookingServiceImpl.cancelBooking()` проверяет дедлайн серверно.

## Запуск

```bash
# Вся инфраструктура + приложение
docker-compose up -d

# Только инфраструктура (для разработки)
docker-compose up -d postgres redis kafka zookeeper

# Приложение локально
./mvnw spring-boot:run
```

## Тесты

```bash
# Все тесты (нужен Docker для Testcontainers)
./mvnw test

# Только unit-тесты
./mvnw test -Dtest="**/unit/**"
```

## API endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/auth/register | Public | Регистрация |
| POST | /api/auth/login | Public | Вход |
| POST | /api/trial-requests | Public | Заявка с лендинга |
| GET | /api/courses | Public | Список курсов |
| GET | /api/site-content | Public | Контент лендинга |
| POST | /api/bookings | STUDENT | Записаться на занятие |
| DELETE | /api/bookings/{id} | STUDENT/MANAGER | Отменить запись |
| GET | /api/bookings/my | STUDENT | Мои записи |
| GET | /api/courses/my | STUDENT | Мои абонементы |
| POST | /api/courses/{id}/purchase | STUDENT | Купить курс |
| GET | /api/teacher/schedule | TEACHER | Расписание преподавателя |
| GET | /api/manager/trial-requests | MANAGER | Заявки на пробный урок |
| POST | /api/manager/courses | MANAGER | Создать курс |
| POST | /api/manager/schedules | MANAGER | Создать слот расписания |
| PUT | /api/admin/site-content/{key} | ADMIN | Обновить контент сайта |

## Мониторинг
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Kafka UI: http://localhost:8090
- Health: http://localhost:8080/actuator/health
