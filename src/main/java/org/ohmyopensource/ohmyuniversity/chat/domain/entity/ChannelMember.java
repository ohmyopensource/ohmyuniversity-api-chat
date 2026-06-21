package org.ohmyopensource.ohmyuniversity.chat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a member of a chat channel in OhMyUniversity!.
 *
 * <p>Members are added automatically by Kafka consumers when the core service
 * publishes {@code enrollment.discovered} (students) or {@code teaching-assignment.discovered}
 * (professors) events. They are never added directly via REST.
 *
 * <p>Membership is scoped to a single {@link ChatChannel} and identified by
 * an opaque {@code userId} string referencing the OhMyU user UUID from the core service. No foreign
 * key to the core service is enforced.
 *
 * <p>Soft delete: when a member leaves or is removed, {@code leftAt} is set
 * to the current timestamp. The row is kept for audit purposes. Active membership is identified by
 * {@code leftAt IS NULL}.
 *
 * <p>The unique constraint on {@code (channel_id, user_id)} prevents duplicate
 * memberships and protects against replayed Kafka events.
 */
@Entity
@Table(
    name = "channel_member",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_channel_member",
            columnNames = {"channel_id", "user_id"}
        )
    }
)
public class ChannelMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  /**
   * The chat channel this membership belongs to. Loaded lazily to avoid unnecessary joins in
   * membership-only queries.
   */
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "channel_id", nullable = false)
  private ChatChannel channel;

  /**
   * Opaque identifier of the user in the core service (OhMyU UUID as string).
   */
  @NotBlank
  @Column(name = "user_id", nullable = false)
  private String userId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private MemberRole role;

  @Column(name = "muted", nullable = false)
  private boolean muted = false;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;

  /**
   * Sets {@code joinedAt} to the current timestamp before the entity is first persisted.
   */
  @PrePersist
  void onCreate() {
    joinedAt = Instant.now();
  }

  // ============ Getters | Setters | Bool ============

  public UUID getId() {
    return id;
  }

  public ChatChannel getChannel() {
    return channel;
  }
  public void setChannel(ChatChannel channel) {
    this.channel = channel;
  }

  public String getUserId() {
    return userId;
  }
  public void setUserId(String userId) {
    this.userId = userId;
  }

  public MemberRole getRole() {
    return role;
  }
  public void setRole(MemberRole role) {
    this.role = role;
  }

  public boolean isMuted() {
    return muted;
  }
  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }
  public Instant getLeftAt() {
    return leftAt;
  }

  public void setLeftAt(Instant leftAt) {
    this.leftAt = leftAt;
  }
}