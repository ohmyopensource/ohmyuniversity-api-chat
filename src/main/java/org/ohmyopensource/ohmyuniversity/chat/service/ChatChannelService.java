package org.ohmyopensource.ohmyuniversity.chat.service;

import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.repository.ChatChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing {@link ChatChannel} lifecycle.
 *
 * Channels are created by the Kafka consumer CourseChannelRequestedConsumer
 * and never created directly via REST. This service provides the shared logic
 * used by both consumers and REST controllers.
 */
@Service
@Transactional(readOnly = true)
public class ChatChannelService {

  private final ChatChannelRepository chatChannelRepository;

  public ChatChannelService(ChatChannelRepository chatChannelRepository) {
    this.chatChannelRepository = chatChannelRepository;
  }

  /**
   * Find a channel by its internal UUID.
   * Used by REST controllers and WebSocket handlers.
   *
   * @param channelId the channel UUID
   * @return the channel if found
   */
  public Optional<ChatChannel> findById(UUID channelId) {
    return chatChannelRepository.findById(channelId);
  }

  /**
   * Find a channel by its external ID provided by the core service.
   * Used by Kafka consumers to retrieve the channel after creation.
   *
   * @param externalChannelId the external channel ID
   * @return the channel if found
   */
  public Optional<ChatChannel> findByExternalChannelId(String externalChannelId) {
    return chatChannelRepository.findByExternalChannelId(externalChannelId);
  }

  /**
   * Create a new channel.
   *
   * This method is idempotent — if a channel with the same externalChannelId
   * already exists, it returns the existing one without creating a duplicate.
   * This protects against replayed Kafka events.
   *
   * @param channel the channel to create (id must be null — set by JPA)
   * @return the persisted channel (existing or newly created)
   */
  @Transactional
  public ChatChannel createIfAbsent(ChatChannel channel) {
    return chatChannelRepository
        .findByExternalChannelId(channel.getExternalChannelId())
        .orElseGet(() -> chatChannelRepository.save(channel));
  }

  /**
   * Transition a channel to a new lifecycle status.
   *
   * @param channelId the channel UUID
   * @param newStatus the target status
   * @return the updated channel
   * @throws IllegalArgumentException if the channel does not exist
   * @throws IllegalStateException    if the transition is not valid
   */
  @Transactional
  public ChatChannel transitionStatus(UUID channelId, ChannelStatus newStatus) {
    ChatChannel channel = chatChannelRepository.findById(channelId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Channel not found: " + channelId));

    validateTransition(channel.getStatus(), newStatus);
    channel.setStatus(newStatus);
    return chatChannelRepository.save(channel);
  }

  // ================================
  // Private helpers
  // ================================

  private void validateTransition(ChannelStatus current, ChannelStatus next) {
    boolean valid = switch (current) {
      case ACTIVE -> next == ChannelStatus.READ_ONLY;
      case READ_ONLY -> next == ChannelStatus.ARCHIVED;
      case ARCHIVED -> next == ChannelStatus.DELETED;
      case DELETED -> false;
    };
    if (!valid) {
      throw new IllegalStateException(
          "Invalid status transition: " + current + " → " + next);
    }
  }
}