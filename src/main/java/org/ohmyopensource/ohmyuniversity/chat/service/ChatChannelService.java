package org.ohmyopensource.ohmyuniversity.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.repository.ChatChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing {@link ChatChannel} lifecycle in OhMyUniversity!.
 *
 * <p>Channels are created exclusively by the Kafka consumer
 * {@code CourseEditionDiscoveredConsumer} and never via REST directly. This service provides the
 * shared logic used by both consumers and REST controllers.
 *
 * <p>Channel lifecycle:
 * - {@code ACTIVE} — channel is open for messages
 * - {@code READ_ONLY} — channel is closed for new messages but still readable
 * - {@code ARCHIVED} — channel is archived
 * - {@code DELETED} — channel is pending deletion
 *
 * <p>The transition from {@code ACTIVE} to {@code READ_ONLY} is triggered automatically
 * by {@link #closeExpiredChannels()} when {@code closesAt} is reached.
 */
@Service
@Transactional(readOnly = true)
public class ChatChannelService {

  private static final Logger log = LoggerFactory.getLogger(ChatChannelService.class);

  private final ChatChannelRepository chatChannelRepository;

  // ============ Constructor ============

  /**
   * Constructs the service with the required repository.
   *
   * @param chatChannelRepository repository for {@link ChatChannel} persistence
   */
  public ChatChannelService(ChatChannelRepository chatChannelRepository) {
    this.chatChannelRepository = chatChannelRepository;
  }

  // ============ Class Methods ============

  /**
   * Finds a channel by its internal UUID.
   *
   * @param channelId the channel UUID
   * @return the channel if found
   */
  public Optional<ChatChannel> findById(UUID channelId) {
    return chatChannelRepository.findById(channelId);
  }

  /**
   * Finds a channel by its deterministic external identifier provided by the core service.
   *
   * @param externalChannelId the external channel ID
   * @return the channel if found
   */
  public Optional<ChatChannel> findByExternalChannelId(String externalChannelId) {
    return chatChannelRepository.findByExternalChannelId(externalChannelId);
  }

  /**
   * Creates a new channel if one with the same {@code externalChannelId} does not already exist.
   *
   * <p>This method is idempotent: replayed Kafka events for the same channel
   * return the existing channel without creating a duplicate.
   *
   * <p>{@code defaultExpiresAt} and {@code closesAt} are calculated automatically
   * by {@link ChatChannel#onCreate()} from {@code academicYear} and {@code semester}.
   *
   * @param channel the channel to create; {@code id} must be null (set by JPA)
   * @return the persisted channel (existing or newly created)
   */
  @Transactional
  public ChatChannel createIfAbsent(ChatChannel channel) {
    return chatChannelRepository
        .findByExternalChannelId(channel.getExternalChannelId())
        .orElseGet(() -> chatChannelRepository.save(channel));
  }

  /**
   * Advances the {@code closesAt} timestamp for a channel.
   *
   * <p>This method allows a professor ({@code TEACHER_ADMIN}) to close the channel
   * earlier than the calculated default. The requested timestamp must not be: - in the past - after
   * {@code defaultExpiresAt}
   *
   * <p>Important: {@code closesAt} can only be moved earlier (advanced), never later.
   * This method does not allow extending the channel beyond its academic TTL.
   *
   * @param channelId   the channel UUID
   * @param newClosesAt the new closing timestamp requested by the professor
   * @return the updated channel
   * @throws IllegalArgumentException if the channel does not exist
   * @throws IllegalArgumentException if {@code newClosesAt} is in the past
   * @throws IllegalArgumentException if {@code newClosesAt} is after {@code defaultExpiresAt}
   * @throws IllegalStateException    if the channel is not in {@code ACTIVE} status
   */
  @Transactional
  public ChatChannel setClosesAt(UUID channelId, Instant newClosesAt) {
    ChatChannel channel = chatChannelRepository.findById(channelId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Channel not found: " + channelId));

    if (channel.getStatus() != ChannelStatus.ACTIVE) {
      throw new IllegalStateException(
          "Cannot change closesAt on a non-active channel: " + channel.getStatus());
    }

    Instant now = Instant.now();
    if (newClosesAt.isBefore(now)) {
      throw new IllegalArgumentException("closesAt cannot be in the past");
    }

    if (channel.getDefaultExpiresAt() != null
        && newClosesAt.isAfter(channel.getDefaultExpiresAt())) {
      throw new IllegalArgumentException(
          "closesAt cannot be after defaultExpiresAt: " + channel.getDefaultExpiresAt());
    }

    channel.setClosesAt(newClosesAt);
    log.info("ChatChannelService: closesAt updated for channel={} newClosesAt={}",
        channelId, newClosesAt);
    return chatChannelRepository.save(channel);
  }

  /**
   * Transitions a channel to a new lifecycle status.
   *
   * <p>Valid transitions:
   * - {@code ACTIVE} -> {@code READ_ONLY} - {@code READ_ONLY} -> {@code ARCHIVED} -
   * {@code ARCHIVED} -> {@code DELETED}
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
    log.info("ChatChannelService: channel={} transitioned {} → {}",
        channelId, channel.getStatus(), newStatus);
    return chatChannelRepository.save(channel);
  }

  /**
   * Scheduled job that closes all active channels whose {@code closesAt} has passed.
   *
   * <p>Runs every hour. For each expired active channel, transitions the status
   * to {@link ChannelStatus#READ_ONLY}.
   *
   * <p>This job controls only the OhMyU chat channel lifecycle — the course itself
   * remains active on Cineca and Moodle indefinitely.
   */
  @Scheduled(fixedDelay = 3_600_000)
  @Transactional
  public void closeExpiredChannels() {
    List<ChatChannel> expired = chatChannelRepository
        .findByStatusAndClosesAtBefore(ChannelStatus.ACTIVE, Instant.now());

    if (expired.isEmpty()) {
      return;
    }

    log.info("ChatChannelService: closing {} expired channels", expired.size());
    for (ChatChannel channel : expired) {
      channel.setStatus(ChannelStatus.READ_ONLY);
      chatChannelRepository.save(channel);
      log.debug("ChatChannelService: channel={} transitioned ACTIVE → READ_ONLY", channel.getId());
    }
  }

  /**
   * Validates that the requested status transition is allowed.
   *
   * @param current the current channel status
   * @param next    the requested target status
   * @throws IllegalStateException if the transition is not permitted
   */
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