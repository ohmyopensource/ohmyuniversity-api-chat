package org.ohmyopensource.ohmyuniversity.chat.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.CourseEditionDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.EnrollmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.TeachingAssignmentDiscoveredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

/**
 * Kafka consumer configuration for the chat microservice.
 *
 * <p>Each topic has its own typed {@link ConsumerFactory} and
 * {@link ConcurrentKafkaListenerContainerFactory} so that {@link JacksonJsonDeserializer} knows
 * exactly which class to deserialize into without needing type headers.
 *
 * <p>{@link ErrorHandlingDeserializer} wraps each deserializer so that malformed
 * messages are logged and skipped instead of crashing the consumer thread.
 *
 * <p>All consumers belong to the {@code ohmyuniversity-chat} consumer group and
 * read from the earliest available offset to ensure no events are missed after a service restart.
 */
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  /**
   * Builds the shared base consumer properties used by all consumer factories.
   *
   * <p>Sets the bootstrap server address, consumer group ID, and offset reset policy.
   *
   * @return map of base Kafka consumer configuration properties
   */
  private Map<String, Object> baseProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "ohmyuniversity-chat");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return props;
  }

  /**
   * Creates a typed {@link ConsumerFactory} for the given event class.
   *
   * <p>Uses {@link JacksonJsonDeserializer} with trusted packages restricted to the
   * chat event package, wrapped in an {@link ErrorHandlingDeserializer} to prevent consumer thread
   * crashes on malformed payloads.
   *
   * @param <T>        the event type
   * @param targetType the class to deserialize Kafka messages into
   * @return configured consumer factory for the given type
   */
  private <T> ConsumerFactory<String, T> consumerFactory(Class<T> targetType) {
    JacksonJsonDeserializer<T> jsonDeserializer = new JacksonJsonDeserializer<>(targetType);
    jsonDeserializer.addTrustedPackages(
        "org.ohmyopensource.ohmyuniversity.chat.kafka.event"
    );

    ErrorHandlingDeserializer<T> errorHandlingDeserializer =
        new ErrorHandlingDeserializer<>(jsonDeserializer);

    return new DefaultKafkaConsumerFactory<>(
        baseProps(),
        new StringDeserializer(),
        errorHandlingDeserializer
    );
  }

  /**
   * Creates a {@link ConcurrentKafkaListenerContainerFactory} for the given event class.
   *
   * @param <T>        the event type
   * @param targetType the class to deserialize Kafka messages into
   * @return configured listener container factory for the given type
   */
  private <T> ConcurrentKafkaListenerContainerFactory<String, T> containerFactory(
      Class<T> targetType) {
    ConcurrentKafkaListenerContainerFactory<String, T> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory(targetType));
    return factory;
  }

  /**
   * Listener container factory for the {@code course-edition.discovered} topic.
   *
   * <p>Consumed by {@code CourseEditionDiscoveredConsumer} to create chat channels
   * for newly discovered course editions.
   *
   * @return configured factory for {@link CourseEditionDiscoveredEvent}
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, CourseEditionDiscoveredEvent>
  courseEditionDiscoveredContainerFactory() {
    return containerFactory(CourseEditionDiscoveredEvent.class);
  }

  /**
   * Listener container factory for the {@code enrollment.discovered} topic.
   *
   * <p>Consumed by {@code EnrollmentDiscoveredConsumer} to add students
   * as members of the corresponding chat channel.
   *
   * @return configured factory for {@link EnrollmentDiscoveredEvent}
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, EnrollmentDiscoveredEvent>
  enrollmentDiscoveredContainerFactory() {
    return containerFactory(EnrollmentDiscoveredEvent.class);
  }

  /**
   * Listener container factory for the {@code teaching-assignment.discovered} topic.
   *
   * <p>Consumed by {@code TeachingAssignmentDiscoveredConsumer} to add professors
   * as admin members of the corresponding chat channel.
   *
   * @return configured factory for {@link TeachingAssignmentDiscoveredEvent}
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, TeachingAssignmentDiscoveredEvent>
  teachingAssignmentDiscoveredContainerFactory() {
    return containerFactory(TeachingAssignmentDiscoveredEvent.class);
  }
}