package org.ohmyopensource.ohmyuniversity.chat.dto;

import java.time.Instant;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;

/**
 * Response DTO representing a chat channel.
 *
 * <p>Returned by {@code GET /api/v1/chat/channels/{channelId}} and by
 * {@code PATCH /api/v1/chat/channels/{channelId}/closes-at} after a successful closing timestamp
 * update.
 *
 * <p>Includes TTL fields ({@code defaultExpiresAt}, {@code closesAt}) so that
 * clients can display the channel lifecycle state and inform users when the channel will transition
 * to read-only.
 */
public class ChannelResponse {

  private UUID id;

  /**
   * Deterministic external identifier built by the core service. Format:
   * {@code {course-slug}-{university-slug}-{year}-{semester}}.
   */
  private String externalChannelId;
  private String name;
  private String courseId;
  private String academicYear;
  private String semester;

  /**
   * Current lifecycle status of the channel.
   */
  private ChannelStatus status;

  /**
   * Maximum TTL calculated at channel creation from {@code academicYear} and {@code semester}.
   * Immutable — represents the hard ceiling for how long the channel can stay active.
   */
  private Instant defaultExpiresAt;

  /**
   * Effective closing timestamp. Defaults to {@code defaultExpiresAt}. May be advanced (never
   * extended) by a {@code TEACHER_ADMIN} via the REST API.
   */
  private Instant closesAt;
  private Instant archiveAt;
  private Instant deleteAt;

  // ============ Getters | Setters | Bool ============

  public UUID getId() {
    return id;
  }
  public void setId(UUID id) {
    this.id = id;
  }

  public String getExternalChannelId() {
    return externalChannelId;
  }
  public void setExternalChannelId(String externalChannelId) {
    this.externalChannelId = externalChannelId;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public String getCourseId() {
    return courseId;
  }
  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  public String getAcademicYear() {
    return academicYear;
  }
  public void setAcademicYear(String academicYear) {
    this.academicYear = academicYear;
  }

  public String getSemester() {
    return semester;
  }
  public void setSemester(String semester) {
    this.semester = semester;
  }

  public ChannelStatus getStatus() {
    return status;
  }
  public void setStatus(ChannelStatus status) {
    this.status = status;
  }

  public Instant getDefaultExpiresAt() {
    return defaultExpiresAt;
  }
  public void setDefaultExpiresAt(Instant defaultExpiresAt) {
    this.defaultExpiresAt = defaultExpiresAt;
  }

  public Instant getClosesAt() {
    return closesAt;
  }
  public void setClosesAt(Instant closesAt) {
    this.closesAt = closesAt;
  }

  public Instant getArchiveAt() {
    return archiveAt;
  }
  public void setArchiveAt(Instant archiveAt) {
    this.archiveAt = archiveAt;
  }

  public Instant getDeleteAt() {
    return deleteAt;
  }
  public void setDeleteAt(Instant deleteAt) {
    this.deleteAt = deleteAt;
  }
}