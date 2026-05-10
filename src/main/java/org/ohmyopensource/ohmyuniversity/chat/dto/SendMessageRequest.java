package org.ohmyopensource.ohmyuniversity.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload sent by the client over WebSocket to /app/chat.send.
 */
public class SendMessageRequest {

  @NotBlank
  @Size(max = 4000)
  private String content;

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}