-- V3__change_user_roles_to_enum.sql
-- Исправляем проблему с ENUM для Hibernate 6 + NAMED_ENUM

-- 1. Переименовываем тип app_role → approle (если он существует)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'app_role') THEN
        ALTER TYPE app_role RENAME TO approle;
    END IF;
END $$;

-- 2. Меняем тип колонки role на approle
ALTER TABLE user_roles
    ALTER COLUMN role TYPE approle
    USING role::text::approle;

-- 3. Создаём CAST'ы, чтобы Hibernate мог работать без ошибок
CREATE CAST (varchar AS app_role) WITH INOUT AS IMPLICIT;
CREATE CAST (app_role AS varchar) WITH INOUT AS IMPLICIT;
