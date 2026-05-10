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
 * Represents a member of a chat channel.
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

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "channel_id", nullable = false)
  private ChatChannel channel;

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

  @PrePersist
  void onCreate() {
    joinedAt = Instant.now();
  }

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