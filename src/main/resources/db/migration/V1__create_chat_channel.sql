-- =============================================================
-- V1 - Create chat_channel table
--
-- Maps to: ChatChannel.java
-- Referenced by: channel_member.channel_id (FK)
-- Referenced by: MongoDB messages.channelId (opaque string, no FK)
-- =============================================================

CREATE TABLE chat_channel
(
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    external_channel_id VARCHAR     NOT NULL,
    name                VARCHAR     NOT NULL,
    course_id           VARCHAR     NOT NULL,
    academic_year       VARCHAR     NOT NULL,
    semester            VARCHAR     NOT NULL,
    status              VARCHAR     NOT NULL,
    archive_at          TIMESTAMP,
    delete_at           TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,

    CONSTRAINT pk_chat_channel PRIMARY KEY (id),
    CONSTRAINT uq_chat_channel_external_id UNIQUE (external_channel_id),
    CONSTRAINT chk_chat_channel_status CHECK (status IN ('ACTIVE', 'READ_ONLY', 'ARCHIVED', 'DELETED'))
);

COMMENT ON TABLE chat_channel IS 'Academic chat channels, one per course edition.';
COMMENT ON COLUMN chat_channel.external_channel_id IS 'Deterministic ID from core service. Format: {course-slug}-{university-slug}-{year}-{semester}.';
COMMENT ON COLUMN chat_channel.course_id IS 'Opaque reference to the course in ohmyuniversity-core. Never queried cross-service by chat.';
COMMENT ON COLUMN chat_channel.status IS 'Lifecycle state: ACTIVE | READ_ONLY | ARCHIVED | DELETED.';
COMMENT ON COLUMN chat_channel.archive_at IS 'Timestamp at which the channel transitions to READ_ONLY.';
COMMENT ON COLUMN chat_channel.delete_at IS 'Timestamp at which the channel is purged. Must match expireAt on MongoDB messages.';