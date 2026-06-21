package org.ohmyopensource.ohmyuniversity.chat.domain.entity;

/**
 * Channel-scoped role assigned to a {@link ChannelMember} in OhMyUniversity!.
 *
 * <p>Roles are scoped to a single channel — the same user can hold different
 * roles across different channels (e.g. {@code STUDENT} in one course channel and {@code TUTOR} in
 * another).
 *
 * <p>Roles are assigned automatically by Kafka consumers:
 * - {@code STUDENT} is assigned by {@code EnrollmentDiscoveredConsumer} when a student enrollment
 * is discovered via Cineca sync. - {@code TEACHER_ADMIN} is assigned by
 * {@code TeachingAssignmentDiscoveredConsumer} when a titular professor assignment is discovered
 * via Cineca sync. - {@code TUTOR} is reserved for future use and must be assigned manually.
 */
public enum MemberRole {

  /**
   * Standard enrolled student. Can send and read messages in the channel.
   */
  STUDENT,

  /**
   * Titular professor with administrative rights on the channel. Can upload material, mute members,
   * pin messages, archive the channel, and advance the channel closing timestamp via the REST API.
   */
  TEACHER_ADMIN,

  /**
   * Teaching assistant or tutor. Can send and read messages and assist students, but does not hold
   * moderation or administrative rights.
   */
  TUTOR
}