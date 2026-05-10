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
 * This service is not transactional because MongoDB standalone (non-replica-set)
 * does not support multi-document transactions.
 */
@Service
public class ChatMessageService {

  private static final int DEFAULT_PAGE_SIZE = 20;

  private final ChatMessageRepository chatMessageRepository;

  public ChatMessageService(ChatMessageRepository chatMessageRepository) {
    this.chatMessageRepository = chatMessageRepository;
  }

  /**
   * Persist a new message in a channel.
   *
   * The message ID is generated here (UUID) rather than delegated to MongoDB
   * to ensure consistency with PostgreSQL UUIDs and to make the ID predictable
   * for deduplication if needed in future.
   *
   * The expireAt field is copied from the parent channel's deleteAt so that
   * MongoDB's TTL index purges messages exactly when the channel is deleted.
   * If deleteAt is null (channel has no expiry), expireAt is also null and the
   * message lives forever until manual cleanup.
   *
   * @param channel  the parent channel (used for channelId and expireAt)
   * @param senderId the opaque user ID from the JWT header
   * @param content  the message text
   * @return the persisted message
   * @throws IllegalStateException if the channel is not ACTIVE
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
   * Retrieve paginated message history for a channel, sorted oldest-first.
   * Used by GET /api/channels/{channelId}/messages.
   *
   * @param channelId the channel UUID
   * @param page      zero-based page number
   * @param size      page size (capped at DEFAULT_PAGE_SIZE if not specified)
   * @return a page of messages sorted by createdAt ascending
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