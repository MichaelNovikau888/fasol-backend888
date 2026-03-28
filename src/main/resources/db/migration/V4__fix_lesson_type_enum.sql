DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lesson_type') THEN
CREATE CAST (varchar AS lesson_type) WITH INOUT AS IMPLICIT;
CREATE CAST (lesson_type AS varchar) WITH INOUT AS IMPLICIT;
END IF;
END $$;