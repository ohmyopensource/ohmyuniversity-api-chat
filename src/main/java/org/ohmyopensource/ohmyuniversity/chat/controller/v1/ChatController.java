package org.ohmyopensource.ohmyuniversity.chat.controller.v1;

import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.chat.domain.document.ChatMessage;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.dto.ChatMessageResponse;
import org.ohmyopensource.ohmyuniversity.chat.dto.SendMessageRequest;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP controller for real-time chat messaging.
 *
 * <p>Handles messages sent by clients over the STOMP protocol to the
 * {@code /app/chat.send/{channelId}} destination. After persisting the message,
 * broadcasts the response to all subscribers of {@code /topic/channel.{channelId}}.
 *
 * <p>The {@code userId} is always read from the {@code X-User-Id} header injected
 * by the API gateway after JWT validation — it is never taken from the client payload.
 *
 * <p>Message flow:
 * - Client sends STOMP SEND frame to {@code /app/chat.send/{channelId}}
 * - Controller validates the channel exists and the sender is an active member
 * - Message is persisted via {@link ChatMessageService}
 * - Persisted message is broadcast to {@code /topic/channel.{channelId}}
 */
@Controller
public class ChatController {

  private static final Logger log = LoggerFactory.getLogger(ChatController.class);

  /**
   * Name of the STOMP header carrying the authenticated user identifier,
   * injected by the API gateway after JWT validation.
   */
  private static final String USER_ID_HEADER = "X-User-Id";

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;
  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  // ============ Constructor ============

  /**
   * Constructs the controller with all required services.
   *
   * @param chatChannelService   service for channel lookup
   * @param channelMemberService service for membership validation
   * @param chatMessageService   service for message persistence
   * @param messagingTemplate    STOMP messaging template for broadcasting
   */
  public ChatController(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService,
      ChatMessageService chatMessageService,
      SimpMessagingTemplate messagingTemplate) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
    this.chatMessageService = chatMessageService;
    this.messagingTemplate = messagingTemplate;
  }

  // ============ Class Methods ============

  /**
   * Handles an incoming chat message from a WebSocket client.
   *
   * <p>The message is silently dropped (with a warning log) if any of the
   * following conditions are met:
   * - {@code channelId} is not a valid UUID
   * - the channel does not exist
   * - the sender is not an active member of the channel
   *
   * <p>On success, the persisted message is broadcast to all subscribers
   * of {@code /topic/channel.{channelId}}.
   *
   * @param channelId the target channel UUID extracted from the STOMP destination variable
   * @param userId    the sender's user ID from the gateway-injected header
   * @param request   the message payload containing the message content
   */
  @MessageMapping("/chat.send/{channelId}")
  public void sendMessage(
      @DestinationVariable String channelId,
      @Header(USER_ID_HEADER) String userId,
      @Payload SendMessageRequest request) {

    UUID channelUuid;
    try {
      channelUuid = UUID.fromString(channelId);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid channelId format received over WebSocket: {}", channelId);
      return;
    }

    ChatChannel channel = chatChannelService.findById(channelUuid).orElse(null);
    if (channel == null) {
      log.warn("Message rejected — channel not found: channelId={}", channelId);
      return;
    }

    if (!channelMemberService.isActiveMember(channelUuid, userId)) {
      log.warn("Message rejected — user is not an active member: "
          + "userId={} channelId={}", userId, channelId);
      return;
    }

    ChatMessage saved = chatMessageService.sendMessage(channel, userId, request.getContent());

    ChatMessageResponse response = toResponse(saved);
    messagingTemplate.convertAndSend(
        "/topic/channel." + channelId,
        response
    );

    log.debug("Message broadcast: channelId={} senderId={} messageId={}",
        channelId, userId, saved.getId());
  }

  /**
   * Maps a {@link ChatMessage} document to a {@link ChatMessageResponse} DTO.
   *
   * @param message the persisted message document
   * @return the response DTO broadcast to channel subscribers
   */
  private ChatMessageResponse toResponse(ChatMessage message) {
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