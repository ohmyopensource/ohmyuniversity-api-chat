package org.ohmyopensource.ohmyuniversity.chat.service;

import java.time.Instant;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.document.ChatMessage;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.repository.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service for managing {@link ChatMessage} documents in MongoDB.
 *
 * <p>This service is intentionally not annotated with {@code @Transactional}
 * because MongoDB standalone instances (non-replica-set) do not support multi-document
 * transactions. Each operation is atomic at the document level.
 *
 * <p>Messages are append-only — no updates or manual deletes are performed.
 * Automatic cleanup is handled by the MongoDB TTL index on {@code expireAt}, which is aligned with
 * the owning channel's {@code deleteAt} timestamp.
 */
@Service
public class ChatMessageService {

  /**
   * Default page size used when the caller does not specify a valid size.
   */
  private static final int DEFAULT_PAGE_SIZE = 20;

  private final ChatMessageRepository chatMessageRepository;

  // ============ Constructor ============

  /**
   * Constructs the service with the required repository.
   *
   * @param chatMessageRepository repository for {@link ChatMessage} persistence
   */
  public ChatMessageService(ChatMessageRepository chatMessageRepository) {
    this.chatMessageRepository = chatMessageRepository;
  }

  // ============ Class Methods ============

  /**
   * Persists a new message in a channel.
   *
   * <p>The message ID is generated as a UUID string rather than delegated to MongoDB
   * to ensure format consistency with PostgreSQL UUIDs and to support future deduplication if
   * needed.
   *
   * <p>The {@code expireAt} field is set to the parent channel's {@code deleteAt}
   * so that MongoDB's TTL index purges messages exactly when the channel is deleted. If
   * {@code deleteAt} is null (the channel has no deletion schedule), {@code expireAt} is also null
   * and the message is retained until manual cleanup.
   *
   * @param channel  the parent channel, used for {@code channelId} and {@code expireAt}
   * @param senderId the opaque user ID from the gateway-injected header
   * @param content  the message text content
   * @return the persisted {@link ChatMessage}
   * @throws IllegalStateException if the channel status is not {@link ChannelStatus#ACTIVE}
   */
  public ChatMessage sendMessage(ChatChannel channel, String senderId, String content) {
    if (channel.getStatus() != ChannelStatus.ACTIVE) {
      throw new IllegalStateException(
          "Cannot send messages to a channel with status: " + channel.getStatus());
    }

    ChatMessage message = new ChatMessage();
    message.setId(UUID.randomUUID().toString());
    message.setChannelId(channel.getId().toString());
    message.setSenderId(senderId);
    message.setContent(content);
    message.setCreatedAt(Instant.now());
    message.setEdited(false);
    message.setExpireAt(channel.getDeleteAt());

    return chatMessageRepository.save(message);
  }

  /**
   * Returns paginated message history for a channel, sorted oldest-first.
   *
   * <p>Oldest-first ordering matches the natural reading order of a chat conversation.
   * Used by {@code ChannelRestController} to serve
   * {@code GET /api/v1/chat/channels/{channelId}/messages}.
   *
   * @param channelId the channel UUID
   * @param page      zero-based page number
   * @param size      page size; falls back to {@link #DEFAULT_PAGE_SIZE} if not positive
   * @return a page of messages sorted by {@code createdAt} ascending
   */
  public Page<ChatMessage> getHistory(UUID channelId, int page, int size) {
    int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
    PageRequest pageable = PageRequest.of(
        page,
        pageSize,
        Sort.by("createdAt").ascending()
    );
    return chatMessageRepository.findByChannelIdOrderByCreatedAtAsc(
        channelId.toString(),
        pageable
    );
  }
}