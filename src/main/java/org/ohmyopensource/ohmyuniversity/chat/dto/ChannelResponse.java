package org.ohmyopensource.ohmyuniversity.chat.dto;

import java.time.Instant;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;

/**
 * Response DTO for {@code GET /api/v1/channels/{channelId}}.
 */
public class ChannelResponse {

  private UUID id;
  private String externalChannelId;
  private String name;
  private String courseId;
  private String academicYear;
  private String semester;
  private ChannelStatus status;
  private Instant defaultExpiresAt;
  private Instant closesAt;
  private Instant archiveAt;
  private Instant deleteAt;

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