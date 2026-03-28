-- V4__fix_lesson_type_enum.sql

-- 1. Переименовываем ENUM lesson_type → lessontype (если нужно)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lesson_type') THEN
        ALTER TYPE lesson_type RENAME TO lessontype;
    END IF;
END $$;

-- 2. Меняем тип колонки в таблице schedules
ALTER TABLE schedules
    ALTER COLUMN lesson_type TYPE lessontype
    USING lesson_type::text::lessontype;

-- 3. Создаём CAST'ы для Hibernate
CREATE CAST (varchar AS lesson_type) WITH INOUT AS IMPLICIT;
CREATE CAST (lesson_type AS varchar) WITH INOUT AS IMPLICIT;
