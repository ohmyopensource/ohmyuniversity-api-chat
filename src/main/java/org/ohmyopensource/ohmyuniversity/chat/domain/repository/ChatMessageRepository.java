package org.ohmyopensource.ohmyuniversity.chat.domain.repository;

import org.ohmyopensource.ohmyuniversity.chat.domain.document.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ChatMessage} documents stored in MongoDB.
 *
 * Messages are append-only in Sprint 1 — no updates, no deletes
 * (TTL index on expireAt handles cleanup automatically).
 *
 * History is always paginated to avoid loading entire channel
 * history into memory. Default page size is defined at the call site
 * in ChatMessageService.
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

  /**
   * Retrieve paginated message history for a channel, sorted oldest-first.
   * Used by GET /api/channels/{channelId}/messages.
   *
   * Sorting oldest-first matches the natural reading order of a chat.
   * The caller passes a {@link Pageable} with page number, size, and
   * sort direction — typically Sort.by("createdAt").ascending().
   *
   * @param channelId the channel UUID as string (matches ChatChannel.id)
   * @param pageable  pagination and sorting parameters
   * @return a page of messages
   */
  Page<ChatMessage> findByChannelIdOrderByCreatedAtAsc(String channelId, Pageable pageable);
}