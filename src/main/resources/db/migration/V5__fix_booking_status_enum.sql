-- V5__fix_booking_status_enum.sql

-- 1. Переименовываем ENUM booking_status → bookingstatus (если он ещё старый)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status') THEN
        ALTER TYPE booking_status RENAME TO bookingstatus;
    END IF;
END $$;

-- 2. Меняем тип колонки status на правильный
ALTER TABLE bookings
    ALTER COLUMN status TYPE bookingstatus
    USING status::text::bookingstatus;

-- 3. Создаём CAST'ы для Hibernate (@JdbcTypeCode NAMED_ENUM)
CREATE CAST (varchar AS bookingstatus) WITH INOUT AS IMPLICIT;
CREATE CAST (bookingstatus AS varchar) WITH INOUT AS IMPLICIT;
