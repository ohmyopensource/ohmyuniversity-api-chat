package org.ohmyopensource.ohmyuniversity.chat.kafka.event;

/**
 * Payload of the {@code professor.assigned} Kafka event.
 * Published by ohmyuniversity-core when a professor is assigned to a course.
 *
 * @param userId            opaque user ID from the core service
 * @param externalChannelId the channel to add the professor to
 */
public record ProfessorAssignedEvent(
    String userId,
    String externalChannelId
) {
}