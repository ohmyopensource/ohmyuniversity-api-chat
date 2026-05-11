package org.ohmyopensource.ohmyuniversity.chat.controller;

import java.util.List;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.document.ChatMessage;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelMember;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChannelMemberResponse;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChannelResponse;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChatMessageResponse;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatMessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for channel metadata and message history.
 *
 * These endpoints are for read-only access — real-time messaging
 * goes through the WebSocket controller.
 *
 * All endpoints require the caller to be an active member of the channel.
 * The userId is read from the X-User-Id header forwarded by the gateway.
 */
@RestController
@RequestMapping("/api/channels")
public class ChannelRestController {

  private static final String USER_ID_HEADER = "X-User-Id";
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 50;

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;
  private final ChatMessageService chatMessageService;

  public ChannelRestController(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService,
      ChatMessageService chatMessageService) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
    this.chatMessageService = chatMessageService;
  }

  /**
   * Get channel metadata.
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @return 200 with channel data, 404 if not found, 403 if not a member
   */
  @GetMapping("/{channelId}")
  public ResponseEntity<ChannelResponse> getChannel(
      @PathVariable UUID channelId,
      @RequestHeader(USER_ID_HEADER) String userId) {

    ChatChannel channel = chatChannelService.findById(channelId).orElse(null);
    if (channel == null) {
      return ResponseEntity.notFound().build();
    }

    if (!channelMemberService.isActiveMember(channelId, userId)) {
      return ResponseEntity.status(403).build();
    }

    return ResponseEntity.ok(toChannelResponse(channel));
  }

  /**
   * Get active members of a channel.
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @return 200 with member list, 404 if channel not found, 403 if not a member
   */
  @GetMapping("/{channelId}/members")
  public ResponseEntity<List<ChannelMemberResponse>> getMembers(
      @PathVariable UUID channelId,
      @RequestHeader(USER_ID_HEADER) String userId) {

    ChatChannel channel = chatChannelService.findById(channelId).orElse(null);
    if (channel == null) {
      return ResponseEntity.notFound().build();
    }

    if (!channelMemberService.isActiveMember(channelId, userId)) {
      return ResponseEntity.status(403).build();
    }

    List<ChannelMemberResponse> members = channelMemberService
        .findActiveMembers(channelId)
        .stream()
        .map(this::toMemberResponse)
        .toList();

    return ResponseEntity.ok(members);
  }

  /**
   * Get paginated message history for a channel, sorted oldest-first.
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @param page      zero-based page number (default 0)
   * @param size      page size (default 20, max 50)
   * @return 200 with paginated messages, 404 if not found, 403 if not a member
   */
  @GetMapping("/{channelId}/messages")
  public ResponseEntity<Page<ChatMessageResponse>> getMessages(
      @PathVariable UUID channelId,
      @RequestHeader(USER_ID_HEADER) String userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    ChatChannel channel = chatChannelService.findById(channelId).orElse(null);
    if (channel == null) {
      return ResponseEntity.notFound().build();
    }

    if (!channelMemberService.isActiveMember(channelId, userId)) {
      return ResponseEntity.status(403).build();
    }

    int cappedSize = Math.min(size, MAX_PAGE_SIZE);
    Page<ChatMessageResponse> messages = chatMessageService
        .getHistory(channelId, page, cappedSize)
        .map(this::toMessageResponse);

    return ResponseEntity.ok(messages);
  }

  // ================================
  // Private mapping helpers
  // ================================

  private ChannelResponse toChannelResponse(ChatChannel channel) {
    ChannelResponse response = new ChannelResponse();
    response.setId(channel.getId());
    response.setExternalChannelId(channel.getExternalChannelId());
    response.setName(channel.getName());
    response.setCourseId(channel.getCourseId());
    response.setAcademicYear(channel.getAcademicYear());
    response.setSemester(channel.getSemester());
    response.setStatus(channel.getStatus());
    response.setArchiveAt(channel.getArchiveAt());
    response.setDeleteAt(channel.getDeleteAt());
    return response;
  }

  private ChannelMemberResponse toMemberResponse(ChannelMember member) {
    ChannelMemberResponse response = new ChannelMemberResponse();
    response.setId(member.getId());
    response.setUserId(member.getUserId());
    response.setRole(member.getRole());
    response.setMuted(member.isMuted());
    response.setJoinedAt(member.getJoinedAt());
    return response;
  }

  private ChatMessageResponse toMessageResponse(ChatMessage message) {
    ChatMessageResponse response = new ChatMessageResponse();
    response.setMessageId(message.getId());
    response.setChannelId(message.getChannelId());
    response.setSenderId(message.getSenderId());
    response.setContent(message.getContent());
    response.setCreatedAt(message.getCreatedAt());
    response.setEdited(message.isEdited());
    return response;
  }
}