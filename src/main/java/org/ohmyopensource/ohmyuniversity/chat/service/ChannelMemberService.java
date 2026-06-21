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
 * Service for managing {@link ChannelMember} membership.
 *
 * Members are added automatically by Kafka consumers when the core service
 * publishes enrollment or assignment events.
 */
@Service
@Transactional(readOnly = true)
public class ChannelMemberService {

  private final ChannelMemberRepository channelMemberRepository;

  public ChannelMemberService(ChannelMemberRepository channelMemberRepository) {
    this.channelMemberRepository = channelMemberRepository;
  }

  /**
   * Returns all active members of a channel.
   * Used by GET /api/channels/{channelId}/members.
   *
   * @param channelId the channel UUID
   * @return list of active members, empty list if none
   */
  public List<ChannelMember> findActiveMembers(UUID channelId) {
    return channelMemberRepository.findByChannelIdAndLeftAtIsNull(channelId);
  }

  /**
   * Find the active membership for a specific user in a channel.
   * Used for permission checks before allowing message sending or moderation.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the JWT header
   * @return the active membership if it exists
   */
  public Optional<ChannelMember> findActiveMember(UUID channelId, String userId) {
    return channelMemberRepository
        .findByChannelIdAndUserIdAndLeftAtIsNull(channelId, userId);
  }

  /**
   * Check whether a user is an active member of a channel.
   * Lightweight version of findActiveMember for permission-only checks.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the JWT header
   * @return true if the user is an active member
   */
  public boolean isActiveMember(UUID channelId, String userId) {
    return channelMemberRepository
        .existsByChannelIdAndUserIdAndLeftAtIsNull(channelId, userId);
  }

  /**
   * Checks whether a user holds a specific role in a channel.
   *
   * @param channelId the channel UUID
   * @param userId    the opaque user ID from the JWT header
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
   * Add a user to a channel with the given role.
   *
   * This method is idempotent — if the user is already an active member,
   * it returns the existing membership without creating a duplicate.
   * This protects against replayed Kafka events.
   *
   * @param channel the parent channel (must be already persisted)
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
   * Remove a user from a channel by setting leftAt to now.
   *
   * This is a soft delete — the membership row is kept for audit purposes.
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