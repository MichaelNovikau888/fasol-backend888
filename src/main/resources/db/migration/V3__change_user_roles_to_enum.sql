DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'app_role') THEN
CREATE CAST (varchar AS app_role) WITH INOUT AS IMPLICIT;
CREATE CAST (app_role AS varchar) WITH INOUT AS IMPLICIT;
END IF;
END $$;