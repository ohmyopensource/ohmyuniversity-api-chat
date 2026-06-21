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
 * MongoDB document representing a single chat message in OhMyUniversity!.
 *
 * <p>Messages are stored in the {@code messages} collection and are scoped
 * to a specific chat channel via {@code channelId}. The channel identifier
 * is stored as an opaque string referencing the PostgreSQL {@code chat_channel.id}
 * — there is no enforced foreign key between MongoDB and PostgreSQL.
 *
 * <p>Automatic expiry: the {@code expireAt} field is backed by a MongoDB TTL index
 * ({@code expireAfter = "0s"}), which instructs MongoDB to delete the document
 * when {@code expireAt} is reached. This value should be set at creation time
 * to match the owning channel's {@code deleteAt} timestamp, ensuring messages
 * are purged together with the channel lifecycle.
 *
 * <p>Edit tracking: the {@code edited} flag is set to {@code true} when the
 * message content is modified after the initial send. {@code editedAt} records
 * the timestamp of the last edit.
 */
@Document(collection = "messages")
public class ChatMessage {

  /**
   * MongoDB-generated document identifier (ObjectId as string).
   */
  @Id
  private String id;

  /**
   * Identifier of the chat channel this message belongs to.
   * Corresponds to {@code chat_channel.id} in PostgreSQL (UUID as string).
   * Indexed for efficient per-channel queries.
   */
  @NotBlank
  @Indexed
  @Field("channelId")
  private String channelId;

  /**
   * Opaque identifier of the user who sent the message.
   * Corresponds to the OhMyU user UUID from the core service.
   */
  @NotBlank
  @Field("senderId")
  private String senderId;

  @NotBlank
  @Field("content")
  private String content;

  /**
   * Optional list of attachment references (e.g. file URLs or storage keys).
   * Null or empty if the message has no attachments.
   */
  @Field("attachments")
  private List<String> attachments;

  @NotNull
  @Field("createdAt")
  private Instant createdAt;

  @Field("edited")
  private boolean edited = false;

  @Field("editedAt")
  private Instant editedAt;

  /**
   * TTL expiry timestamp for automatic MongoDB document deletion.
   *
   * <p>Backed by a MongoDB TTL index with {@code expireAfter = "0s"}, which causes
   * MongoDB to delete this document when {@code expireAt} is reached.
   * Should be set to the owning channel's {@code deleteAt} value so messages
   * are purged in sync with the channel lifecycle.
   */
  @Indexed(expireAfter = "0s")
  @Field("expireAt")
  private Instant expireAt;

  // ============ Getters | Setters | Bool ============

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