package org.ohmyopensource.ohmyuniversity.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelMember;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;
import org.ohmyopensource.ohmyuniversity.chat.domain.repository.ChannelMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing {@link ChannelMember} lifecycle in OhMyUniversity!.
 *
 * <p>Members are added automatically by Kafka consumers when the core service
 * publishes {@code enrollment.discovered} (students) or {@code teaching-assignment.discovered}
 * (professors) events. They are never added directly via REST.
 *
 * <p>Membership removal is soft-delete only — rows are retained for audit
 * purposes with {@code leftAt} set to the removal timestamp. All queries filter on
 * {@code leftAt IS NULL} to return only active memberships.
 */
@Service
@Transactional(readOnly = true)
public class ChannelMemberService {

  private final ChannelMemberRepository channelMemberRepository;

  // ============ Constructor ============

  /**
   * Constructs the service with the required repository.
   *
   * @param channelMemberRepository repository for {@link ChannelMember} persistence
   */
  public ChannelMemberService(ChannelMemberRepository channelMemberRepository) {
    this.channelMemberRepository = channelMemberRepository;
  }

  // ============ Class Methods ============

  /**
   * Returns all active members of a channel.
   *
   * <p>Used by {@code ChannelRestController} to serve
   * {@code GET /api/v1/chat/channels/{channelId}/members}.
   *
   * @param channelId the channel UUID
   * @return list of active members; empty list if none
   */
  public List<ChannelMember> findActiveMembers(UUID channelId) {
    return channelMemberRepository.findByChannelIdAndLeftAtIsNull(channelId);
  }

  /**
   * Returns the active membership for a specific user in a channel.
   *
   * <p>Used for permission checks before allowing message sending or moderation.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the gateway-injected header
   * @return the active membership if it exists
   */
  public Optional<ChannelMember> findActiveMember(UUID channelId, String userId) {
    return channelMemberRepository
        .findByChannelIdAndUserIdAndLeftAtIsNull(channelId, userId);
  }

  /**
   * Checks whether a user is an active member of a channel.
   *
   * <p>Lightweight alternative to {@link #findActiveMember} for permission-only
   * checks where the membership details are not needed.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the gateway-injected header
   * @return {@code true} if the user is an active member
   */
  public boolean isActiveMember(UUID channelId, String userId) {
    return channelMemberRepository
        .existsByChannelIdAndUserIdAndLeftAtIsNull(channelId, userId);
  }

  /**
   * Checks whether a user holds a specific role in a channel.
   *
   * <p>Used by {@code ChannelRestController} to authorise role-restricted
   * operations such as advancing the channel closing timestamp.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the gateway-injected header
   * @param role      the role to check
   * @return {@code true} if the user is an active member with the given role
   */
  public boolean hasRole(UUID channelId, String userId, MemberRole role) {
    return channelMemberRepository
        .findByChannelIdAndUserIdAndLeftAtIsNull(channelId, userId)
        .map(member -> member.getRole() == role)
        .orElse(false);
  }

  /**
   * Adds a user to a channel with the given role.
   *
   * <p>This method is idempotent — if the user is already an active member,
   * the existing membership is returned without creating a duplicate. This protects against
   * replayed {@code enrollment.discovered} and {@code teaching-assignment.discovered} Kafka
   * events.
   *
   * @param channel the parent channel; must already be persisted
   * @param userId  the opaque user ID from the core service
   * @param role    the role to assign within this channel
   * @return the persisted membership (existing or newly created)
   */
  @Transactional
  public ChannelMember addMember(ChatChannel channel, String userId, MemberRole role) {
    return channelMemberRepository
        .findByChannelIdAndUserIdAndLeftAtIsNull(channel.getId(), userId)
        .orElseGet(() -> {
          ChannelMember member = new ChannelMember();
          member.setChannel(channel);
          member.setUserId(userId);
          member.setRole(role);
          return channelMemberRepository.save(member);
        });
  }

  /**
   * Removes a user from a channel by setting {@code leftAt} to the current timestamp.
   *
   * <p>This is a soft delete — the membership row is retained for audit purposes.
   * If the user is not an active member, this method does nothing.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID
   */
  @Transactional
  public void removeMember(UUID channelId, String userId) {
    channelMemberRepository
        .findByChannelIdAndUserIdAndLeftAtIsNull(channelId, userId)
        .ifPresent(member -> {
          member.setLeftAt(Instant.now());
          channelMemberRepository.save(member);
        });
  }
}