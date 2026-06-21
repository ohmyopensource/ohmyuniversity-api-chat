package org.ohmyopensource.ohmyuniversity.chat.domain.entity;

/**
 * Lifecycle states of a {@link ChatChannel} in OhMyUniversity!.
 *
 * <p>Valid transitions:
 * - {@code ACTIVE} → {@code READ_ONLY}: triggered automatically by the scheduled job in
 * {@code ChatChannelService} when {@code closesAt} is reached, or manually advanced by a
 * {@code TEACHER_ADMIN} via the REST API. - {@code READ_ONLY} → {@code ARCHIVED}: manual action by
 * a {@code TEACHER_ADMIN} or a future scheduled job when {@code archive_at} is reached. -
 * {@code ARCHIVED} → {@code DELETED}: triggered when {@code delete_at} is reached; MongoDB messages
 * are purged via the TTL index on {@code expireAt}.
 *
 * <p>No reverse transitions are permitted. The lifecycle is strictly monotonic.
 */
public enum ChannelStatus {

  /**
   * Channel is open. Members can send and read messages.
   */
  ACTIVE,

  /**
   * Channel is closed for new messages. Members can still read the message history. Triggered when
   * {@code closesAt} is reached.
   */
  READ_ONLY,

  /**
   * Channel is archived. No longer visible in the active channel list, but message history remains
   * accessible.
   */
  ARCHIVED,

  /**
   * Channel and all associated messages have been permanently deleted. MongoDB documents are purged
   * via the TTL index on {@code expireAt}.
   */
  DELETED
}