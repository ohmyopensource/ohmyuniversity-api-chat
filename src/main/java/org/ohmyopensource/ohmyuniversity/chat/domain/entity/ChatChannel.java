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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Represents an academic chat channel in OhMyUniversity!.
 *
 * <p>Chat channels are created automatically by the Kafka consumer
 * {@code CourseEditionDiscoveredConsumer} when a new course edition is discovered
 * via Cineca sync. They are never created manually via REST.
 *
 * <p>Each channel has a bounded lifecycle tied to the academic semester:
 * <ul>
 *   <li>{@code defaultExpiresAt} — the maximum TTL, calculated at creation time from
 *       {@code academicYear} and {@code semester}. Immutable after creation.</li>
 *   <li>{@code closesAt} — the effective closing timestamp. Defaults to
 *       {@code defaultExpiresAt}. A professor ({@code TEACHER_ADMIN}) may advance this
 *       date but never extend it beyond {@code defaultExpiresAt}.</li>
 * </ul>
 *
 * <p>Important: these TTL fields control only the OhMyU chat channel.
 * The course itself remains active on Cineca and Moodle indefinitely.
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

  /**
   * Maximum lifetime of this channel, calculated from {@code academicYear} and {@code semester}
   * at creation time.
   *
   * <p>Semester 1 (autumn): end of February of the following year (exam session buffer included).
   * Semester 2 (spring): end of September of the same year.
   *
   * <p>This field is immutable after creation — it represents the hard ceiling
   * for how long this channel can stay active.
   */
  @Column(name = "default_expires_at", updatable = false)
  private Instant defaultExpiresAt;

  /**
   * Effective closing timestamp for this channel.
   *
   * <p>Defaults to {@link #defaultExpiresAt} at creation time.
   * A professor ({@code TEACHER_ADMIN}) may set an earlier date via the REST API,
   * but may never extend it beyond {@link #defaultExpiresAt}.
   *
   * <p>When {@code closesAt} is reached, a scheduled job transitions the channel
   * to {@link ChannelStatus#READ_ONLY}.
   */
  @Column(name = "closes_at")
  private Instant closesAt;

  @Column(name = "archive_at")
  private Instant archiveAt;

  @Column(name = "delete_at")
  private Instant deleteAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Lifecycle hook executed before the entity is first persisted.
   *
   * <p>Sets {@code createdAt}, {@code updatedAt}, and {@code status} defaults.
   * Also calculates {@code defaultExpiresAt} and initialises {@code closesAt}
   * to the same value if not already set.
   */
  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
    if (status == null) {
      status = ChannelStatus.ACTIVE;
    }
    if (defaultExpiresAt == null && academicYear != null && semester != null) {
      defaultExpiresAt = calculateDefaultExpiresAt(academicYear, semester);
    }
    if (closesAt == null) {
      closesAt = defaultExpiresAt;
    }
  }

  /**
   * Lifecycle hook executed before each update.
   *
   * <p>Refreshes {@code updatedAt} to the current timestamp.
   */
  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  /**
   * Calculates the default channel expiry date based on the academic year and semester.
   *
   * <p>Semester 1 (autumn/winter): expires at end of February of the following year,
   * covering the winter exam session (January–February).
   * Semester 2 (spring/summer): expires at end of September of the same year,
   * covering the summer exam session (June–September).
   *
   * @param academicYear four-digit academic year string (e.g. {@code "2026"})
   * @param semester     semester number string ({@code "1"} or {@code "2"})
   * @return the calculated expiry as an {@link Instant} at midnight UTC
   */
  static Instant calculateDefaultExpiresAt(String academicYear, String semester) {
    int year = Integer.parseInt(academicYear);
    LocalDate expiry = "2".equals(semester)
        ? LocalDate.of(year, 9, 30)
        : LocalDate.of(year + 1, 2, 28);
    return expiry.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  // ============ Getters | Setters | Bool ============

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

  public Instant getDefaultExpiresAt() {
    return defaultExpiresAt;
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

  public Instant getCreatedAt() {
    return createdAt;
  }
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}