package org.ohmyopensource.ohmyuniversity.chat.kafka.consumer;

import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.CourseChannelRequestedEvent;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the {@code course.channel.requested} topic.
 */
@Component
public class CourseChannelRequestedConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(CourseChannelRequestedConsumer.class);

  private final ChatChannelService chatChannelService;

  public CourseChannelRequestedConsumer(ChatChannelService chatChannelService) {
    this.chatChannelService = chatChannelService;
  }

  @KafkaListener(
      topics = "course.channel.requested",
      groupId = "ohmyuniversity-chat",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void consume(CourseChannelRequestedEvent event) {
    log.debug("Received course.channel.requested event: externalChannelId={}",
        event.externalChannelId());

    ChatChannel channel = new ChatChannel();
    channel.setExternalChannelId(event.externalChannelId());
    channel.setName(event.name());
    channel.setCourseId(event.courseId());
    channel.setAcademicYear(event.academicYear());
    channel.setSemester(event.semester());
    channel.setStatus(ChannelStatus.ACTIVE);

    ChatChannel saved = chatChannelService.createIfAbsent(channel);

    log.info("Channel ready: id={} externalChannelId={}",
        saved.getId(), saved.getExternalChannelId());
  }
}