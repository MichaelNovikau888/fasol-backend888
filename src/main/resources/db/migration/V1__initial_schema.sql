-- ============================================================
-- Fa Sol School — полная схема БД
-- Воспроизводит оригинальные Supabase-миграции на чистом SQL
-- ============================================================

-- Enums
CREATE TYPE app_role        AS ENUM ('STUDENT','TEACHER','MANAGER','ADMIN');
CREATE TYPE booking_status  AS ENUM ('CONFIRMED','CANCELLED','COMPLETED');
CREATE TYPE lesson_type     AS ENUM ('INDIVIDUAL','GROUP');
CREATE TYPE trial_status    AS ENUM ('NEW','CONTACTED','SCHEDULED','COMPLETED','CANCELLED');

-- Users
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  UNIQUE,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    app_role NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Teachers (отдельная таблица — как в оригинале)
CREATE TABLE teachers (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    bio            TEXT,
    specialization VARCHAR(255),
    avatar_url     TEXT,
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Courses / tariff packages
CREATE TABLE courses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    individual_lessons  INTEGER NOT NULL DEFAULT 0,
    group_lessons       INTEGER NOT NULL DEFAULT 0,
    price               NUMERIC(10,2) NOT NULL DEFAULT 0,
    discount_price      NUMERIC(10,2),
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Schedules — weekly recurring slots
CREATE TABLE schedules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id       UUID NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    course_id        UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    day_of_week      INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    lesson_type      lesson_type NOT NULL,
    max_participants INTEGER NOT NULL DEFAULT 1,
    active           BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_time CHECK (end_time > start_time)
);

CREATE INDEX idx_schedules_teacher ON schedules(teacher_id);

-- Student subscriptions (purchased packages)
CREATE TABLE student_courses (
    id                             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id                     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id                      UUID NOT NULL REFERENCES courses(id),
    individual_lessons_remaining   INTEGER NOT NULL DEFAULT 0,
    group_lessons_remaining        INTEGER NOT NULL DEFAULT 0,
    repeat_purchase                BOOLEAN NOT NULL DEFAULT false,
    paid_online                    BOOLEAN NOT NULL DEFAULT false,
    stripe_payment_id              VARCHAR(255),
    expires_at                     TIMESTAMP,
    version                        BIGINT NOT NULL DEFAULT 0,
    created_at                     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_student_courses_student ON student_courses(student_id);

-- Bookings
CREATE TABLE bookings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    schedule_id      UUID NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    student_course_id UUID REFERENCES student_courses(id) ON DELETE SET NULL,
    booking_date     DATE NOT NULL,
    status           booking_status NOT NULL DEFAULT 'CONFIRMED',
    cancelled_by     UUID REFERENCES users(id),
    cancelled_at     TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_student      ON bookings(student_id);
CREATE INDEX idx_bookings_schedule_date ON bookings(schedule_id, booking_date);

-- Trial requests (from landing page)
CREATE TABLE trial_requests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100),
    phone          VARCHAR(20) NOT NULL,
    wants_whatsapp BOOLEAN NOT NULL DEFAULT false,
    status         trial_status NOT NULL DEFAULT 'NEW',
    notes          TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Site content (landing page managed sections)
CREATE TABLE site_content (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key VARCHAR(100) NOT NULL UNIQUE,
    title       TEXT,
    description TEXT,
    image_url   TEXT,
    content     JSONB DEFAULT '{}',
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Transactional outbox for reliable Kafka publishing
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB NOT NULL,
    processed      BOOLEAN NOT NULL DEFAULT false,
    processed_at   TIMESTAMP,
    retry_count    INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed, created_at)
    WHERE processed = false;

-- Default site content (from original Supabase migration)
INSERT INTO site_content (section_key, title, description, content) VALUES
('feature_rating',   'Лучшая школа в городе по рейтингу', 'Средний рейтинг 5 из 5 — нас любят за атмосферу и результат', '{"yandex_url":"https://yandex.by/maps/"}'),
('feature_method',   'Современная методика преподавания', 'По авторской методике с использованием упражнений Шерил Портер', '{}'),
('feature_teachers', 'Опытные и заботливые преподаватели', 'Профессиональные преподаватели, прошедшие серьёзный отбор', '{}'),
('feature_schedule', 'Удобный график занятий', 'Школа работает 7 дней в неделю с 10:00 до 22:00', '{}');
