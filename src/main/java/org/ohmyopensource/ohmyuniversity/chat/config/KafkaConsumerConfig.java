package org.ohmyopensource.ohmyuniversity.chat.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.CourseChannelRequestedEvent;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.ProfessorAssignedEvent;
import org.ohmyopensource.ohmyuniversity.chat.kafka.event.StudentEnrolledEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

/**
 * Kafka consumer configuration.
 */
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  // ================================
  // Shared base properties
  // ================================

  private Map<String, Object> baseProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "ohmyuniversity-chat");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return props;
  }

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

  private <T> ConcurrentKafkaListenerContainerFactory<String, T> containerFactory(
      Class<T> targetType) {
    ConcurrentKafkaListenerContainerFactory<String, T> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory(targetType));
    return factory;
  }

  // ================================
  // course.channel.requested
  // ================================

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, CourseChannelRequestedEvent>
  courseChannelRequestedContainerFactory() {
    return containerFactory(CourseChannelRequestedEvent.class);
  }

  // ================================
  // student.enrolled
  // ================================

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, StudentEnrolledEvent>
  studentEnrolledContainerFactory() {
    return containerFactory(StudentEnrolledEvent.class);
  }

  // ================================
  // professor.assigned
  // ================================

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, ProfessorAssignedEvent>
  professorAssignedContainerFactory() {
    return containerFactory(ProfessorAssignedEvent.class);
  }
}