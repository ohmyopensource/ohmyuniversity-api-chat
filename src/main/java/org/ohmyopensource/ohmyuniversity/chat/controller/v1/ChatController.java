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
 * WebSocket STOMP controller for real-time chat.
 * The userId is read from the {@code X-User-Id} header forwarded
 * by the API gateway after JWT validation. It is never taken from
 * the client payload directly.
 */
@Controller
public class ChatController {

  private static final Logger log = LoggerFactory.getLogger(ChatController.class);

  private static final String USER_ID_HEADER = "X-User-Id";

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;
  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;

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

  /**
   * Handle an incoming chat message from a client.
   * @param channelId the target channel UUID from the STOMP destination
   * @param userId    the sender's user ID from the gateway header
   * @param request   the message payload
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

  // ================================
  // Private helpers
  // ================================

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