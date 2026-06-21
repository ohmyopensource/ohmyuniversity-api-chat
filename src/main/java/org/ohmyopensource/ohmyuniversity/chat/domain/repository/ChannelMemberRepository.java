package org.ohmyopensource.ohmyuniversity.chat.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ChannelMember} entities.
 *
 * <p>All queries filter on {@code leftAt IS NULL} to return only active memberships.
 * Soft-deleted members ({@code leftAt != null}) are retained for audit purposes but excluded from
 * all operational queries.
 *
 * <p>Custom queries use Spring Data derived query methods to avoid explicit JPQL
 * and keep the code readable and maintainable.
 */
@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

  /**
   * Returns all active members of a channel.
   *
   * <p>Used by {@code ChannelMemberService#findActiveMembers} to serve the
   * REST endpoint {@code GET /api/v1/chat/channels/{channelId}/members}.
   *
   * @param channelId the channel UUID
   * @return list of active members; empty list if none
   */
  List<ChannelMember> findByChannelIdAndLeftAtIsNull(UUID channelId);

  /**
   * Returns the active membership for a specific user in a channel.
   *
   * <p>Used for permission checks before allowing message sending,
   * moderation actions, or member-specific operations.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the gateway-injected header
   * @return the active membership if it exists
   */
  Optional<ChannelMember> findByChannelIdAndUserIdAndLeftAtIsNull(UUID channelId, String userId);

  /**
   * Checks whether a user is already an active member of a channel.
   *
   * <p>Used by {@code ChannelMemberService#addMember} for idempotency —
   * prevents duplicate memberships from replayed {@code enrollment.discovered} or
   * {@code teaching-assignment.discovered} Kafka events.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID
   * @return {@code true} if the user is already an active member
   */
  boolean existsByChannelIdAndUserIdAndLeftAtIsNull(UUID channelId, String userId);
}