package org.ohmyopensource.ohmyuniversity.chat.controller.v1;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.document.ChatMessage;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelMember;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChannelMemberResponse;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChannelResponse;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChatMessageResponse;
import org.ohmyopensource.ohmyuniversity.chat.dto.SetClosesAtRequest;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for channel metadata, member management, and message history.
 *
 * <p>These endpoints are for read and administrative operations — real-time messaging
 * goes through the WebSocket controller.
 *
 * <p>All endpoints require the caller to be an active member of the channel.
 * The {@code userId} is read from the {@code X-User-Id} header forwarded by the gateway.
 */
@RestController
@RequestMapping("/api/v1/chat/channels")
public class ChannelRestController {

  private static final Logger log = LoggerFactory.getLogger(ChannelRestController.class);

  private static final String USER_ID_HEADER = "X-User-Id";
  private static final int MAX_PAGE_SIZE = 50;

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;
  private final ChatMessageService chatMessageService;

  /**
   * Constructs the controller with all required services.
   *
   * @param chatChannelService   service for channel lifecycle management
   * @param channelMemberService service for member management
   * @param chatMessageService   service for message history retrieval
   */
  public ChannelRestController(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService,
      ChatMessageService chatMessageService) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
    this.chatMessageService = chatMessageService;
  }

  /**
   * Returns channel metadata including TTL fields.
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
   * Returns the active members of a channel.
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
   * Returns paginated message history for a channel, sorted oldest-first.
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

  /**
   * Allows a professor ({@code TEACHER_ADMIN}) to advance the closing timestamp
   * of a channel earlier than the default academic TTL.
   *
   * <p>The requested {@code closesAt} must not be in the past and must not exceed
   * {@code defaultExpiresAt}. Only active channels can be updated.
   * Only members with {@link MemberRole#TEACHER_ADMIN} role are authorised.
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @param request   the request body containing the new {@code closesAt} timestamp
   * @return 200 with updated channel data, 404 if not found,
   *         403 if not a TEACHER_ADMIN, 400 if the timestamp is invalid
   */
  @PatchMapping("/{channelId}/closes-at")
  public ResponseEntity<ChannelResponse> setClosesAt(
      @PathVariable UUID channelId,
      @RequestHeader(USER_ID_HEADER) String userId,
      @Valid @RequestBody SetClosesAtRequest request) {

    ChatChannel channel = chatChannelService.findById(channelId).orElse(null);
    if (channel == null) {
      return ResponseEntity.notFound().build();
    }

    if (!channelMemberService.hasRole(channelId, userId, MemberRole.TEACHER_ADMIN)) {
      log.warn("ChannelRestController: unauthorized setClosesAt attempt by userId={} on "
          + "channelId={}", userId, channelId);
      return ResponseEntity.status(403).build();
    }

    try {
      ChatChannel updated = chatChannelService.setClosesAt(channelId, request.getClosesAt());
      return ResponseEntity.ok(toChannelResponse(updated));
    } catch (IllegalArgumentException e) {
      log.warn("ChannelRestController: invalid closesAt request for channelId={}: {}",
          channelId, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (IllegalStateException e) {
      log.warn("ChannelRestController: invalid state for setClosesAt channelId={}: {}",
          channelId, e.getMessage());
      return ResponseEntity.status(409).build();
    }
  }

  /**
   * Maps a {@link ChatChannel} entity to a {@link ChannelResponse} DTO.
   *
   * @param channel the channel entity
   * @return the response DTO
   */
  private ChannelResponse toChannelResponse(ChatChannel channel) {
    ChannelResponse response = new ChannelResponse();
    response.setId(channel.getId());
    response.setExternalChannelId(channel.getExternalChannelId());
    response.setName(channel.getName());
    response.setCourseId(channel.getCourseId());
    response.setAcademicYear(channel.getAcademicYear());
    response.setSemester(channel.getSemester());
    response.setStatus(channel.getStatus());
    response.setDefaultExpiresAt(channel.getDefaultExpiresAt());
    response.setClosesAt(channel.getClosesAt());
    response.setArchiveAt(channel.getArchiveAt());
    response.setDeleteAt(channel.getDeleteAt());
    return response;
  }

  /**
   * Maps a {@link ChannelMember} entity to a {@link ChannelMemberResponse} DTO.
   *
   * @param member the member entity
   * @return the response DTO
   */
  private ChannelMemberResponse toMemberResponse(ChannelMember member) {
    ChannelMemberResponse response = new ChannelMemberResponse();
    response.setId(member.getId());
    response.setUserId(member.getUserId());
    response.setRole(member.getRole());
    response.setMuted(member.isMuted());
    response.setJoinedAt(member.getJoinedAt());
    return response;
  }

  /**
   * Maps a {@link ChatMessage} document to a {@link ChatMessageResponse} DTO.
   *
   * @param message the message document
   * @return the response DTO
   */
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