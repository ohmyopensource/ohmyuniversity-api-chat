package org.ohmyopensource.ohmyuniversity.chat.kafka.event;

/**
 * Payload of the {@code student.enrolled} Kafka event.
 * Published by ohmyuniversity-core when a student enrolls in a course.
 *
 * @param userId            opaque user ID from the core service
 * @param externalChannelId the channel to add the student to
 */
public record StudentEnrolledEvent(
    String userId,
    String externalChannelId
) {
}