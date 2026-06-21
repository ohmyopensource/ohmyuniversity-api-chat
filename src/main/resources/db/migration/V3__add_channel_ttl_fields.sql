-- =============================================================
-- V3 - Add TTL fields to chat_channel
--
-- Adds two timestamp fields to support channel lifecycle management
-- based on academic semester boundaries:
--
-- default_expires_at: calculated at channel creation time from
--   academic_year + semester. Represents the maximum allowed
--   lifetime of the chat channel (end of semester + exam buffer).
--   This field is immutable after creation.
--
-- closes_at: the effective closing timestamp. Defaults to
--   default_expires_at at creation. A professor with TEACHER_ADMIN
--   role may advance this date (never beyond default_expires_at).
--   When closes_at is reached, a scheduled job transitions the
--   channel to READ_ONLY.
-- =============================================================

ALTER TABLE chat_channel
    ADD COLUMN default_expires_at TIMESTAMPTZ,
    ADD COLUMN closes_at          TIMESTAMPTZ;

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON COLUMN chat_channel.default_expires_at IS
    'Maximum channel TTL, calculated from academic_year + semester at creation. Immutable. '
    'Sem 1: end of February next year. Sem 2: end of September same year.';

COMMENT ON COLUMN chat_channel.closes_at IS
    'Effective closing timestamp. Defaults to default_expires_at. '
    'May be advanced (never extended) by a TEACHER_ADMIN. '
    'When reached, scheduled job transitions channel to READ_ONLY.';