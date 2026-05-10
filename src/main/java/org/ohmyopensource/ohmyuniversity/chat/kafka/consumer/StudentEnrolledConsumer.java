package org.ohmyopensource.ohmyuniversity.chat.kafka.consumer;

import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.MemberRole;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.StudentEnrolledEvent;
import org.ohmyopensource.ohmyuniversity.chat.service.ChannelMemberService;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the {@code student.enrolled} topic.
 *
 * The core service publishes this event when a student enrolls in a course.
 * This consumer adds the student as a STUDENT member of the corresponding channel.
 *
 * If the channel does not exist yet (race condition between course.channel.requested
 * and student.enrolled events), the event is logged as a warning and dropped.
 */
@Component
public class StudentEnrolledConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(StudentEnrolledConsumer.class);

  private final ChatChannelService chatChannelService;
  private final ChannelMemberService channelMemberService;

  public StudentEnrolledConsumer(
      ChatChannelService chatChannelService,
      ChannelMemberService channelMemberService) {
    this.chatChannelService = chatChannelService;
    this.channelMemberService = channelMemberService;
  }

  @KafkaListener(
      topics = "student.enrolled",
      groupId = "ohmyuniversity-chat",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void consume(StudentEnrolledEvent event) {
    log.debug("Received student.enrolled event: userId={} externalChannelId={}",
        event.userId(), event.externalChannelId());

    ChatChannel channel = chatChannelService
        .findByExternalChannelId(event.externalChannelId())
        .orElse(null);

    if (channel == null) {
      log.warn("Channel not found for student.enrolled event: externalChannelId={}. "
              + "Event dropped — channel may not have been created yet.",
          event.externalChannelId());
      return;
    }

    channelMemberService.addMember(channel, event.userId(), MemberRole.STUDENT);

    log.info("Student added to channel: userId={} channelId={}",
        event.userId(), channel.getId());
  }
}