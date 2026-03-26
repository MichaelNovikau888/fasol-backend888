-- V7__add_contract_url_to_courses.sql
-- PDF-договор для тарифа.
-- Менеджер загружает PDF в Supabase Storage, URL сохраняется здесь.
-- Студент видит договор в диалоге и должен подтвердить его перед оплатой.

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS contract_url TEXT DEFAULT NULL;

COMMENT ON COLUMN courses.contract_url
    IS 'URL PDF-договора в Supabase Storage. NULL если договор не загружен менеджером.';
