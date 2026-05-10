package org.ohmyopensource.ohmyuniversity.chat.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ChatChannel} entities.
 *
 * Extends JpaRepository to provide standard CRUD operations.
 * Custom queries are added only when needed — prefer derived query
 * methods over JPQL to keep the code readable.
 */
@Repository
public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {

  /**
   * Find a channel by its deterministic external ID provided by the core service.
   * Used by Kafka consumers to look up the channel before adding members,
   * and to prevent duplicate channel creation from replayed events.
   *
   * @param externalChannelId the external channel ID (e.g. programming-1-univpm-2026-1)
   * @return the channel if it exists
   */
  Optional<ChatChannel> findByExternalChannelId(String externalChannelId);

  /**
   * Check whether a channel with the given external ID already exists.
   * Used by CourseChannelRequestedConsumer for idempotency checks
   * before attempting to create a new channel.
   *
   * @param externalChannelId the external channel ID
   * @return true if the channel already exists
   */
  boolean existsByExternalChannelId(String externalChannelId);
}