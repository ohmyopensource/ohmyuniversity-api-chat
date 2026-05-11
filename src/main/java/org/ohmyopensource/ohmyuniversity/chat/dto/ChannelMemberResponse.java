package org.ohmyopensource.ohmyuniversity.chat.dto;

import java.time.Instant;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;

/**
 * Response DTO for a single member in GET /api/channels/{channelId}/members.
 */
public class ChannelMemberResponse {

  private UUID id;
  private String userId;
  private MemberRole role;
  private boolean muted;
  private Instant joinedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
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

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }
}