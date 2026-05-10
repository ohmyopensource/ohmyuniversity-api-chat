package org.ohmyopensource.ohmyuniversity.chat.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

/**
 * Kafka consumer configuration.
 */
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "ohmyuniversity-chat");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    JacksonJsonDeserializer<Object> valueDeserializer = new JacksonJsonDeserializer<>();
    valueDeserializer.addTrustedPackages(
        "org.ohmyopensource.ohmyuniversity.chat.kafka.event"
    );
    valueDeserializer.setUseTypeHeaders(false);

    return new DefaultKafkaConsumerFactory<>(
        props,
        new StringDeserializer(),
        valueDeserializer
    );
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object>
  kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }
}