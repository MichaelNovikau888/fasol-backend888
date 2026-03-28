DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status') THEN
CREATE CAST (varchar AS booking_status) WITH INOUT AS IMPLICIT;
CREATE CAST (booking_status AS varchar) WITH INOUT AS IMPLICIT;
END IF;
END $$;