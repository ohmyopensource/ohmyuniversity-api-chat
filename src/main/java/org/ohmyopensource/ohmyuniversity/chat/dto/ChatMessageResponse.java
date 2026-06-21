package org.ohmyopensource.ohmyuniversity.chat.dto;

import java.time.Instant;

/**
 * Response DTO representing a chat message.
 *
 * <p>Broadcast to all subscribers of {@code /topic/channel.{channelId}}
 * after a message is persisted by {@code ChatMessageService}. Also returned by the REST endpoint
 * {@code GET /api/v1/chat/channels/{channelId}/messages} for paginated history.
 */
public class ChatMessageResponse {

  private String messageId;

  /**
   * Identifier of the channel this message belongs to (UUID as string).
   */
  private String channelId;
  private String senderId;
  private String content;
  private Instant createdAt;
  private boolean edited;

  // ============ Getters | Setters | Bool ============

  public String getMessageId() {
    return messageId;
  }
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getChannelId() {
    return channelId;
  }
  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getSenderId() {
    return senderId;
  }
  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isEdited() {
    return edited;
  }
  public void setEdited(boolean edited) {
    this.edited = edited;
  }
}