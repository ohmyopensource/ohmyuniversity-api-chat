package org.ohmyopensource.ohmyuniversity.chat.domain.entity;

/**
 * Role of a member within a specific chat channel.
 * Roles are channel-scoped — a user can be STUDENT in one channel
 * and TUTOR in another.
 */
public enum MemberRole {

  /** Standard student. Can send and read messages. */
  STUDENT,

  /**
   * Professor with administrative rights on the channel.
   * Can upload material, mute students, pin messages, archive the channel.
   */
  TEACHER_ADMIN,

  /**
   * Teaching assistant or tutor.
   * Can send messages and help students but cannot moderate the channel.
   */
  TUTOR
}