package org.ohmyopensource.ohmyuniversity.chat.domain.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Represents a chat message stored in MongoDB.
 */
@Document(collection = "messages")
public class ChatMessage {

  @Id
  private String id;

  @NotBlank
  @Indexed
  @Field("channelId")
  private String channelId;

  @NotBlank
  @Field("senderId")
  private String senderId;

  @NotBlank
  @Field("content")
  private String content;

  @Field("attachments")
  private List<String> attachments;

  @NotNull
  @Field("createdAt")
  private Instant createdAt;

  @Field("edited")
  private boolean edited = false;

  @Field("editedAt")
  private Instant editedAt;

  @Indexed(expireAfter = "0s")
  @Field("expireAt")
  private Instant expireAt;

  public String getId() {
    return id;
  }

  public void setId(String id) { this.id = id; }

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

  public List<String> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<String> attachments) {
    this.attachments = attachments;
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

  public Instant getEditedAt() {
    return editedAt;
  }

  public void setEditedAt(Instant editedAt) {
    this.editedAt = editedAt;
  }

  public Instant getExpireAt() {
    return expireAt;
  }

  public void setExpireAt(Instant expireAt) {
    this.expireAt = expireAt;
  }
}