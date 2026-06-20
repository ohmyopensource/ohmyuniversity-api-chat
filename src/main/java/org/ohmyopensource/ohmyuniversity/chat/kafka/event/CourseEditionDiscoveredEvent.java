package org.ohmyopensource.ohmyuniversity.chat.kafka.event;

/**
 * Payload of the {@code course-edition.discovered} Kafka event.
 *
 * <p>Published by {@code ohmyuniversity-core} when a Cineca sync reveals a course
 * edition that does not yet have a chat channel in OhMyUniversity!.
 *
 * <p>Shape contract: field names must match exactly those published by the core service,
 * as {@link org.springframework.kafka.support.serializer.JacksonJsonDeserializer} binds
 * by field name without type headers.
 *
 * @param externalChannelId deterministic channel identifier built by the core service;
 *                          format: {@code {course-slug}-{university-slug}-{year}-{semester}}
 *                          (e.g. {@code analisi-i-unimol-2026-1})
 * @param name              human-readable channel name displayed in the chat UI
 *                          (e.g. {@code Analisi I — UNIMOL — 2026/1})
 * @param courseId          opaque Cineca activity identifier ({@code adsceId}) from the core
 *                          service, stored as a string for forward compatibility
 * @param academicYear      academic year as a four-digit string (e.g. {@code "2026"})
 * @param semester          semester number as a string (e.g. {@code "1"} or {@code "2"})
 */
public record CourseEditionDiscoveredEvent(
    String externalChannelId,
    String name,
    String courseId,
    String academicYear,
    String semester
) {
}