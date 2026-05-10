package org.ohmyopensource.ohmyuniversity.chat.kafka.event;

/**
 * Payload of the {@code course.channel.requested} Kafka event.
 * Published by ohmyuniversity-core when a new course edition needs a chat channel.
 *
 * @param externalChannelId deterministic ID — format: {course-slug}-{university-slug}-{year}-{semester}
 * @param name              human-readable channel name
 * @param courseId          opaque reference to the course in the core service
 * @param academicYear      e.g. "2026"
 * @param semester          e.g. "1" or "2"
 */
public record CourseChannelRequestedEvent(
    String externalChannelId,
    String name,
    String courseId,
    String academicYear,
    String semester
) {
}