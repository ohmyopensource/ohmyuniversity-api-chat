package org.ohmyopensource.ohmyuniversity.chat.kafka.consumer;

import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.ProfessorAssignedEvent;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the {@code professor.assigned} topic.
 *
 * The core service publishes this event when a professor is assigned to a course.
 * This consumer adds the professor as a TEACHER_ADMIN member of the corresponding channel,
 * giving them moderation rights (mute, pin, archive).
 *
 * Same race-condition handling as StudentEnrolledConsumer — if the channel
 * does not exist yet, the event is dropped with a warning.
 */
@Component
public class ProfessorAssignedConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(ProfessorAssignedConsumer.class);

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;

  public ProfessorAssignedConsumer(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
  }

  @KafkaListener(
      topics = "professor.assigned",
      groupId = "ohmyuniversity-chat",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void consume(ProfessorAssignedEvent event) {
    log.debug("Received professor.assigned event: userId={} externalChannelId={}",
        event.userId(), event.externalChannelId());

    ChatChannel channel = chatChannelService
        .findByExternalChannelId(event.externalChannelId())
        .orElse(null);

    if (channel == null) {
      log.warn("Channel not found for professor.assigned event: externalChannelId={}. "
              + "Event dropped — channel may not have been created yet.",
          event.externalChannelId());
      return;
    }

    channelMemberService.addMember(channel, event.userId(), MemberRole.TEACHER_ADMIN);

    log.info("Professor added to channel: userId={} channelId={}",
        event.userId(), channel.getId());
  }
}