package org.ohmyopensource.ohmyuniversity.chat.kafka.consumer;

import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChannelStatus;
import org.ohmyopensource.ohmyuniversity.chat.domain.entity.ChatChannel;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.CourseEditionDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.chat.service.ChatChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the {@code course-edition.discovered} topic.
 *
 * <p>Receives integration events published by {@code ohmyuniversity-core} when a Cineca sync
 * reveals a course edition that does not yet have a chat channel in OhMyUniversity!.
 *
 * <p>On each event, this consumer delegates to {@link ChatChannelService#createIfAbsent} to
 * create the channel if it does not already exist. The operation is idempotent: if the channel was
 * already created by a previous event, no duplicate is produced.
 *
 * <p>Ordering constraint: this consumer must process the {@code course-edition.discovered} event
 * for a given {@code externalChannelId} before the corresponding {@code enrollment.discovered} and
 * {@code teaching-assignment.discovered} events arrive. If those events are consumed before the
 * channel exists, they are silently dropped by their respective consumers.
 */
@Component
public class CourseEditionDiscoveredConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(CourseEditionDiscoveredConsumer.class);

  private final ChatChannelService chatChannelService;

  // ============ Constructor ============

  /**
   * Constructs the consumer with the required channel service.
   *
   * @param chatChannelService service responsible for chat channel lifecycle management
   */
  public CourseEditionDiscoveredConsumer(ChatChannelService chatChannelService) {
    this.chatChannelService = chatChannelService;
  }

  // ============ Class Methods ============

  /**
   * Processes a {@code course-edition.discovered} event and ensures the corresponding chat channel
   * exists.
   *
   * <p>Builds a {@link ChatChannel} from the event payload and delegates creation to
   * {@link ChatChannelService#createIfAbsent}. The channel is created with status
   * {@link ChannelStatus#ACTIVE}.
   *
   * @param event the incoming Kafka event payload; must not be null
   */
  @KafkaListener(
      topics = "course-edition.discovered",
      groupId = "ohmyuniversity-chat",
      containerFactory = "courseEditionDiscoveredContainerFactory"
  )
  public void consume(CourseEditionDiscoveredEvent event) {
    log.debug("Received course-edition.discovered event: externalChannelId={}",
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