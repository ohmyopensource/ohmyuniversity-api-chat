package org.ohmyopensource.ohmyuniversity.chat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an academic chat channel.
 */
@Entity
@Table(
    name = "chat_channel",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_chat_channel_external_id",
            columnNames = "external_channel_id"
        )
    }
)
public class ChatChannel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @NotBlank
  @Column(name = "external_channel_id", nullable = false, unique = true)
  private String externalChannelId;

  @NotBlank
  @Column(name = "name", nullable = false)
  private String name;

  @NotBlank
  @Column(name = "course_id", nullable = false)
  private String courseId;

  @NotBlank
  @Column(name = "academic_year", nullable = false)
  private String academicYear;

  @NotBlank
  @Column(name = "semester", nullable = false)
  private String semester;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ChannelStatus status;

  @Column(name = "archive_at")
  private Instant archiveAt;

  @Column(name = "delete_at")
  private Instant deleteAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
    if (status == null) {
      status = ChannelStatus.ACTIVE;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}