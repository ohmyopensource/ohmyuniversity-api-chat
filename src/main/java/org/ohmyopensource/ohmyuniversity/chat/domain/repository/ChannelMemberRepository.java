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
 * All queries filter on leftAt IS NULL to return only active members.
 * Soft-deleted members (leftAt != null) are kept for audit purposes
 * but excluded from all operational queries.
 */
@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

  /**
   * Find all active members of a channel.
   * Used by the REST endpoint GET /api/channels/{channelId}/members.
   *
   * @param channelId the channel UUID
   * @return list of active members, empty list if none
   */
  List<ChannelMember> findByChannelIdAndLeftAtIsNull(UUID channelId);

  /**
   * Find a specific active member in a channel.
   * Used for permission checks before sending messages or moderating.
   *
   * @param channelId the channel UUID
   * @param userId the opaque user ID from JWT
   * @return the active membership if it exists
   */
  Optional<ChannelMember> findByChannelIdAndUserIdAndLeftAtIsNull(UUID channelId, String userId);

  /**
   * Check whether a user is already an active member of a channel.
   * Used by Kafka consumers for idempotency — prevents duplicate memberships
   * from replayed StudentEnrolled or ProfessorAssigned events.
   *
   * @param channelId the channel UUID
   * @param userId the opaque user ID
   * @return true if the user is already an active member
   */
  boolean existsByChannelIdAndUserIdAndLeftAtIsNull(UUID channelId, String userId);
}