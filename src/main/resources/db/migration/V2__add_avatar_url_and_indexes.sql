-- ============================================================
-- V2: добавляем avatar_url к пользователям
--     (поле нужно ProfileController и UserAdminController)
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- Индекс для быстрого поиска пользователей по активности (менеджер)
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);

-- Индекс для ускорения запроса DashboardService.findUpcomingByStudentId
CREATE INDEX IF NOT EXISTS idx_bookings_student_date_status
    ON bookings(student_id, booking_date, status);
