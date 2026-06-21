package org.ohmyopensource.ohmyuniversity.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the OhMyUniversity! chat microservice.
 *
 * <p>Responsible for real-time academic chat channels via WebSocket (STOMP),
 * REST endpoints for channel metadata and message history, and Kafka consumers for channel and
 * membership lifecycle events published by the core service.
 *
 * <p>{@link EnableScheduling} activates the scheduled job
 * {@code ChatChannelService#closeExpiredChannels()} which runs hourly to transition expired active
 * channels to {@code READ_ONLY}.
 */
@SpringBootApplication
@EnableScheduling
public class OhmyuniversityChatApplication {

  /**
   * Starts the Spring Boot application.
   *
   * @param args command-line arguments passed to the Spring application context
   */
  public static void main(String[] args) {
    SpringApplication.run(OhmyuniversityChatApplication.class, args);
  }
}