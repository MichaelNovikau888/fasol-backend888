-- V6__link_trial_to_purchase.sql
-- Связываем покупку курса с заявкой на пробный урок.
-- Это позволяет точно считать воронку:
--   заявка → пришёл на пробное → купил курс

ALTER TABLE student_courses
    ADD COLUMN IF NOT EXISTS trial_request_id UUID
        REFERENCES trial_requests(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_student_courses_trial
    ON student_courses(trial_request_id)
    WHERE trial_request_id IS NOT NULL;

-- Комментарий для понимания: NULL означает что покупка была НЕ после пробного урока
-- (повторная покупка, прямая продажа, офлайн без заявки)
COMMENT ON COLUMN student_courses.trial_request_id
    IS 'Ссылка на заявку на пробный урок, после которого студент купил курс. NULL если покупка без пробного.';
