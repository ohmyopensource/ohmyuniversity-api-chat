package org.ohmyopensource.ohmyuniversity.chat.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Properties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Loads environment variables from .env file into Spring's property sources.
 * Runs before the application context is initialized so that ${VAR} placeholders
 * in application.yml are resolved correctly.
 */
public class DotenvLoader implements EnvironmentPostProcessor, Ordered {

  private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvProperties";

  private static final String[] ENV_KEYS = {
      "SPRING_PROFILES_ACTIVE",
      "POSTGRES_URL",
      "POSTGRES_USERNAME",
      "POSTGRES_PASSWORD",
      "MONGO_HOST",
      "MONGO_PORT",
      "MONGO_DB",
      "MONGO_USERNAME",
      "MONGO_PASSWORD",
      "REDIS_PUBSUB_HOST",
      "REDIS_PUBSUB_PORT",
      "REDIS_PUBSUB_PASSWORD",
      "KAFKA_BOOTSTRAP_SERVERS",
      "JWT_SECRET"
  };

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment,
      SpringApplication application) {

    try {
      Dotenv dotenv = Dotenv.configure()
          .directory(System.getProperty("user.dir"))
          .ignoreIfMissing()
          .ignoreIfMalformed()
          .load();

      Properties props = new Properties();

      for (String key : ENV_KEYS) {
        try {
          String value = dotenv.get(key);
          props.put(key, value);
        } catch (Exception ignored) {
        }
      }

      if (!props.isEmpty()) {
        environment.getPropertySources()
            .addFirst(new PropertiesPropertySource(DOTENV_PROPERTY_SOURCE_NAME, props));
      }

    } catch (Exception e) {
    }
  }
}