package org.ohmyopensource.ohmyuniversity.chat.domain.repository;

import org.ohmyopensource.ohmyuniversity.chat.domain.document.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository for {@link ChatMessage} documents.
 *
 * <p>Messages are append-only — no updates or manual deletes are performed.
 * Automatic cleanup is handled by the MongoDB TTL index on the {@code expireAt} field, which purges
 * documents when their expiry timestamp is reached.
 *
 * <p>Message history is always returned paginated to prevent loading an entire
 * channel history into memory. Page size and sort order are determined by the caller via
 * {@link Pageable}.
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

  /**
   * Returns paginated message history for a channel, sorted oldest-first.
   *
   * <p>Oldest-first ordering matches the natural reading order of a chat conversation.
   * The caller is responsible for providing a {@link Pageable} with the appropriate page number,
   * size, and sort direction — typically {@code Sort.by("createdAt").ascending()}.
   *
   * <p>Used by {@code ChatMessageService#getHistory} to serve the REST endpoint
   * {@code GET /api/v1/chat/channels/{channelId}/messages}.
   *
   * @param channelId the channel UUID as string, matching {@code chat_channel.id} in PostgreSQL
   * @param pageable  pagination and sorting parameters
   * @return a page of messages belonging to the specified channel
   */
  Page<ChatMessage> findByChannelIdOrderByCreatedAtAsc(String channelId, Pageable pageable);
}