package org.ohmyopensource.ohmyuniversity.chat.domain.entity;

/**
 * Lifecycle states of a chat channel.
 * Transitions:
 * ACTIVE -> READ_ONLY (course ends, archive_at reached)
 * READ_ONLY -> ARCHIVED (manual action by TEACHER_ADMIN or scheduled job)
 * ARCHIVED -> DELETED (delete_at reached, messages purged from MongoDB)
 */
public enum ChannelStatus {

  /** Channel is open. Members can send and read messages. */
  ACTIVE,

  /** Course has ended. Members can only read. No new messages allowed. */
  READ_ONLY,

  /** Channel is archived. Visible only in history, not in active list. */
  ARCHIVED,

  /** Channel and all messages have been permanently deleted. */
  DELETED
}