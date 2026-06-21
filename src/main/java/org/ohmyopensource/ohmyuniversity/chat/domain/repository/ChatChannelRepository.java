package org.ohmyopensource.ohmyuniversity.chat.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ChatChannel} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations.
 * Custom queries use Spring Data derived query methods to keep the code readable
 * without explicit JPQL.
 */
@Repository
public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {

  /**
   * Finds a channel by its deterministic external identifier provided by the core service.
   *
   * <p>Used by Kafka consumers to look up the channel before adding members,
   * and to prevent duplicate channel creation from replayed events.
   *
   * @param externalChannelId the external channel ID
   *                          (e.g. {@code analisi-i-unimol-2026-1})
   * @return the channel if it exists
   */
  Optional<ChatChannel> findByExternalChannelId(String externalChannelId);

  /**
   * Checks whether a channel with the given external identifier already exists.
   *
   * <p>Used for idempotency checks before attempting to create a new channel.
   *
   * @param externalChannelId the external channel ID
   * @return {@code true} if the channel already exists
   */
  boolean existsByExternalChannelId(String externalChannelId);

  /**
   * Finds all channels with the given status whose {@code closesAt} is before
   * the specified instant.
   *
   * <p>Used by {@code ChatChannelService#closeExpiredChannels()} to retrieve
   * active channels whose TTL has passed, so they can be transitioned to
   * {@link ChannelStatus#READ_ONLY}.
   *
   * @param status    the channel status to filter by (typically {@link ChannelStatus#ACTIVE})
   * @param threshold the reference instant; channels with {@code closesAt} before this
   *                  are considered expired
   * @return list of matching channels; empty list if none found
   */
  List<ChatChannel> findByStatusAndClosesAtBefore(ChannelStatus status, Instant threshold);
}