package org.ohmyopensource.ohmyuniversity.chat.kafka.event;

/**
 * Payload of the {@code enrollment.discovered} Kafka event.
 *
 * <p>Published by {@code ohmyuniversity-core} when a Cineca sync reveals that a
 * student is enrolled in a course edition tracked by OhMyUniversity!.
 *
 * <p>Shape contract: field names must match exactly those published by the core service,
 * as {@link org.springframework.kafka.support.serializer.JacksonJsonDeserializer} binds
 * by field name without type headers.
 *
 * <p>Ordering constraint: the chat channel identified by {@code externalChannelId} must
 * already exist when this event is consumed. If it does not, the event is silently dropped.
 * The {@code course-edition.discovered} event for the same channel must be processed first.
 *
 * @param userId            OhMyU user UUID string identifying the enrolled student
 * @param externalChannelId deterministic channel identifier matching the corresponding
 *                          {@code course-edition.discovered} event
 */
public record EnrollmentDiscoveredEvent(
    String userId,
    String externalChannelId
) {
}