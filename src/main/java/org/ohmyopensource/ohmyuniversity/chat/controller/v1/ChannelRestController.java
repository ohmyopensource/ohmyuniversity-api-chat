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
 * REST controller for channel metadata, member listing, message history, and channel lifecycle
 * management.
 *
 * <p>These endpoints cover read and administrative operations.
 * Real-time messaging is handled separately by the WebSocket controller.
 *
 * <p>All endpoints require the caller to be an active member of the channel.
 * The {@code userId} is extracted from the {@code X-User-Id} header injected by the API gateway
 * after JWT validation — it is never read directly from a client-supplied value.
 *
 * <p>Exposed endpoints:
 * - {@code GET  /api/v1/chat/channels/{channelId}}              — channel metadata
 * - {@code GET  /api/v1/chat/channels/{channelId}/members}      — active members
 * - {@code GET  /api/v1/chat/channels/{channelId}/messages}     — paginated history
 * - {@code PATCH /api/v1/chat/channels/{channelId}/closes-at}   — advance TTL (TEACHER_ADMIN)
 */
@RestController
@RequestMapping("/api/v1/chat/channels")
public class ChannelRestController {

  private static final Logger log = LoggerFactory.getLogger(ChannelRestController.class);

  /**
   * Name of the HTTP header carrying the authenticated user identifier, injected by the API gateway
   * after JWT validation.
   */
  private static final String USER_ID_HEADER = "X-User-Id";

  /**
   * Maximum number of messages returned per page to prevent excessive payload sizes.
   */
  private static final int MAX_PAGE_SIZE = 50;

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;
  private final ChatMessageService chatMessageService;

  // ============ Constructor ============

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

  // ============ Class Methods ============

  /**
   * Returns channel metadata including TTL fields ({@code defaultExpiresAt}, {@code closesAt}).
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @return 200 with channel data, 404 if not found, 403 if not an active member
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
   * Returns the list of currently active members of a channel.
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @return 200 with member list, 404 if channel not found, 403 if not an active member
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
   * <p>Page size is capped at {@link #MAX_PAGE_SIZE} regardless of the requested value.
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @param page      zero-based page number (default 0)
   * @param size      page size (default 20, capped at {@value #MAX_PAGE_SIZE})
   * @return 200 with paginated messages, 404 if not found, 403 if not an active member
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
   * Allows a professor ({@code TEACHER_ADMIN}) to advance the closing timestamp of a channel
   * earlier than the calculated academic TTL.
   *
   * <p>Validation rules enforced by {@link ChatChannelService#setClosesAt}:
   * - {@code closesAt} must not be in the past
   * - {@code closesAt} must not exceed {@code defaultExpiresAt}
   * - The channel must be in {@code ACTIVE} status
   *
   * @param channelId the channel UUID
   * @param userId    the caller's user ID from the gateway header
   * @param request   the request body containing the new {@code closesAt} timestamp
   * @return 200 with updated channel data, 400 if the timestamp violates validation rules, 403 if
   * the caller is not a {@code TEACHER_ADMIN}, 404 if the channel does not exist, 409 if the
   * channel is not in a state that allows this operation
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