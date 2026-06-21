package org.ohmyopensource.ohmyuniversity.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request payload for {@code PATCH /api/v1/channels/{channelId}/closes-at}.
 *
 * <p>Allows a professor ({@code TEACHER_ADMIN}) to advance the closing timestamp
 * of an active chat channel. The requested timestamp must not be in the past and must not exceed
 * the channel's {@code defaultExpiresAt}.
 */
public class SetClosesAtRequest {

  @NotNull
  private Instant closesAt;

  // ============ Getters | Setters | Bool ============

  public Instant getClosesAt() {
    return closesAt;
  }

  public void setClosesAt(Instant closesAt) {
    this.closesAt = closesAt;
  }
}