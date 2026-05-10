-- =============================================================
-- V2 - Create channel_member table
--
-- Maps to: ChannelMember.java
-- FK: channel_id → chat_channel(id)
-- =============================================================

CREATE TABLE channel_member
(
    id         UUID      NOT NULL DEFAULT gen_random_uuid(),
    channel_id UUID      NOT NULL,
    user_id    VARCHAR   NOT NULL,
    role       VARCHAR   NOT NULL,
    muted      BOOLEAN   NOT NULL DEFAULT FALSE,
    joined_at  TIMESTAMP NOT NULL,
    left_at    TIMESTAMP,

    CONSTRAINT pk_channel_member PRIMARY KEY (id),
    CONSTRAINT fk_channel_member_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id) ON DELETE CASCADE,
    CONSTRAINT uq_channel_member UNIQUE (channel_id, user_id),
    CONSTRAINT chk_channel_member_role CHECK (role IN ('STUDENT', 'TEACHER_ADMIN', 'TUTOR'))
);

CREATE INDEX idx_channel_member_channel_id ON channel_member (channel_id);
CREATE INDEX idx_channel_member_user_id ON channel_member (user_id);

COMMENT ON TABLE channel_member IS 'Members of a chat channel. One row per (channel, user) pair.';
COMMENT ON COLUMN channel_member.user_id IS 'Opaque reference to the user in ohmyuniversity-core.';
COMMENT ON COLUMN channel_member.role IS 'Channel-scoped role: STUDENT | TEACHER_ADMIN | TUTOR.';
COMMENT ON COLUMN channel_member.muted IS 'Quick mute flag. Detailed mute info (reason, expiry) in ChannelMute — Sprint 2.';
COMMENT ON COLUMN channel_member.left_at IS 'Set when the member leaves or is removed. NULL means still active.';