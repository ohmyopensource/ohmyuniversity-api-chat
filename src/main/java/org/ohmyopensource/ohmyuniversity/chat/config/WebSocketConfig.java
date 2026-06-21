package org.ohmyopensource.ohmyuniversity.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket and STOMP message broker configuration for the chat microservice.
 *
 * <p>Clients establish a WebSocket connection to {@code /ws} using the STOMP protocol.
 * Once connected, they can:
 * - Subscribe to {@code /topic/channel.{channelId}} to receive real-time messages
 *       broadcast to a specific chat channel
 * - Send messages to {@code /app/chat.send} to publish a new message to a channel
 *
 * <p>The in-memory simple broker is used for local message routing. For production
 * deployments with multiple instances, this should be replaced with an external broker (e.g.
 * RabbitMQ or Redis Pub/Sub) to support cross-instance fan-out.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  // ============ Override Methods ============

  /**
   * Configures the in-memory message broker and application destination prefix.
   *
   * <p>The simple broker handles subscriptions to {@code /topic/**} destinations.
   * Client-sent frames targeting {@code /app/**} are routed to {@code @MessageMapping} handler
   * methods in controllers.
   *
   * @param registry the message broker registry provided by Spring
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  /**
   * Registers the STOMP WebSocket endpoint.
   *
   * <p>Clients connect to {@code /ws} to initiate a STOMP session.
   * All origin patterns are currently allowed — this should be restricted to trusted domains in
   * production.
   *
   * @param registry the STOMP endpoint registry provided by Spring
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*");
  }
}