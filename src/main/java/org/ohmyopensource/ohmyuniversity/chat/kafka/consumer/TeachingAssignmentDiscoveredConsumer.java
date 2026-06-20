package org.ohmyopensource.ohmyuniversity.chat.kafka.consumer;

import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.TeachingAssignmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the {@code teaching-assignment.discovered} topic.
 *
 * <p>Receives integration events published by {@code ohmyuniversity-core} when a Cineca sync
 * reveals that a professor is the titular holder ({@code titolareFlg}) of a course edition tracked
 * by OhMyUniversity!.
 *
 * <p>On each event, this consumer adds the professor as a {@link MemberRole#TEACHER_ADMIN} member
 * of the corresponding chat channel via {@link ChannelMemberService#addMember}.
 *
 * <p>Ordering constraint: the chat channel identified by {@code externalChannelId} must already
 * exist before this event is processed. If the channel is not found, the event is silently dropped
 * with a warning log. This is by design: the {@code course-edition.discovered} event must be
 * consumed first to guarantee the channel exists.
 */
@Component
public class TeachingAssignmentDiscoveredConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(TeachingAssignmentDiscoveredConsumer.class);

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;

  // ============ Constructor ============

  /**
   * Constructs the consumer with the required channel and member services.
   *
   * @param chatChannelService   service for chat channel lookup
   * @param channelMemberService service for adding members to chat channels
   */
  public TeachingAssignmentDiscoveredConsumer(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
  }

  // ============ Class Methods ============

  /**
   * Processes a {@code teaching-assignment.discovered} event and adds the professor as an admin
   * member of the corresponding chat channel.
   *
   * <p>If the channel identified by {@code externalChannelId} does not exist,
   * the event is dropped and a warning is logged. No retry or dead-letter mechanism is applied —
   * the ordering contract with the producer is the primary safeguard against this scenario.
   *
   * @param event the incoming Kafka event payload; must not be null
   */
  @KafkaListener(
      topics = "teaching-assignment.discovered",
      groupId = "ohmyuniversity-chat",
      containerFactory = "teachingAssignmentDiscoveredContainerFactory"
  )
  public void consume(TeachingAssignmentDiscoveredEvent event) {
    log.debug("Received teaching-assignment.discovered event: userId={} externalChannelId={}",
        event.userId(), event.externalChannelId());

    ChatChannel channel = chatChannelService
        .findByExternalChannelId(event.externalChannelId())
        .orElse(null);

    if (channel == null) {
      log.warn("Channel not found for teaching-assignment.discovered event: "
              + "externalChannelId={}. Event dropped — channel may not have been created yet.",
          event.externalChannelId());
      return;
    }

    channelMemberService.addMember(channel, event.userId(), MemberRole.TEACHER_ADMIN);

    log.info("Professor added to channel: userId={} channelId={}",
        event.userId(), channel.getId());
  }
}