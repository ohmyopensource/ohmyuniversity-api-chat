package org.ohmyopensource.ohmyuniversity.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload sent by a client over WebSocket to {@code /app/chat.send/{channelId}}.
 *
 * <p>Validated by the STOMP message handler in {@code ChatController} before processing.
 * The sender identity is never taken from this payload — it is always read from the
 * {@code X-User-Id} header injected by the API gateway.
 */
public class SendMessageRequest {

  /**
   * Text content of the message. Must not be blank and must not exceed 4000 characters.
   */
  @NotBlank
  @Size(max = 4000)
  private String content;

  // ============ Getters | Setters | Bool ============

  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }
}