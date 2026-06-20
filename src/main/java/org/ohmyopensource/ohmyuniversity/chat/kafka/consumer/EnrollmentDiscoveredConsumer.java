package org.ohmyopensource.ohmyuniversity.chat.kafka.consumer;

import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.EnrollmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the {@code enrollment.discovered} topic.
 *
 * <p>Receives integration events published by {@code ohmyuniversity-core} when a Cineca sync
 * reveals that a student is enrolled in a course edition tracked by OhMyUniversity!.
 *
 * <p>On each event, this consumer adds the student as a {@link MemberRole#STUDENT} member
 * of the corresponding chat channel via {@link ChannelMemberService#addMember}.
 *
 * <p>Ordering constraint: the chat channel identified by {@code externalChannelId} must already
 * exist before this event is processed. If the channel is not found, the event is silently dropped
 * with a warning log. This is by design: the {@code course-edition.discovered} event must be
 * consumed first to guarantee the channel exists.
 */
@Component
public class EnrollmentDiscoveredConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(EnrollmentDiscoveredConsumer.class);

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;

  // ============ Constructor ============

  /**
   * Constructs the consumer with the required channel and member services.
   *
   * @param chatChannelService   service for chat channel lookup
   * @param channelMemberService service for adding members to chat channels
   */
  public EnrollmentDiscoveredConsumer(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
  }

  // ============ Class Methods ============

  /**
   * Processes an {@code enrollment.discovered} event and adds the student to the corresponding chat
   * channel.
   *
   * <p>If the channel identified by {@code externalChannelId} does not exist,
   * the event is dropped and a warning is logged. No retry or dead-letter mechanism is applied —
   * the ordering contract with the producer is the primary safeguard against this scenario.
   *
   * @param event the incoming Kafka event payload; must not be null
   */
  @KafkaListener(
      topics = "enrollment.discovered",
      groupId = "ohmyuniversity-chat",
      containerFactory = "enrollmentDiscoveredContainerFactory"
  )
  public void consume(EnrollmentDiscoveredEvent event) {
    log.debug("Received enrollment.discovered event: userId={} externalChannelId={}",
        event.userId(), event.externalChannelId());

    ChatChannel channel = chatChannelService
        .findByExternalChannelId(event.externalChannelId())
        .orElse(null);

    if (channel == null) {
      log.warn("Channel not found for enrollment.discovered event: externalChannelId={}. "
              + "Event dropped — channel may not have been created yet.",
          event.externalChannelId());
      return;
    }

    channelMemberService.addMember(channel, event.userId(), MemberRole.STUDENT);

    log.info("Student added to channel: userId={} channelId={}",
        event.userId(), channel.getId());
  }
}