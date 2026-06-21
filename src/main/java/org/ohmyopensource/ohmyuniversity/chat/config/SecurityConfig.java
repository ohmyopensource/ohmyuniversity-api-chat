package org.ohmyopensource.ohmyuniversity.chat.config;

import org.ohmyopensource.ohmyuniversity.chat.security.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the chat microservice.
 *
 * <p>The chat service operates behind the OhMyUniversity API Gateway, which is
 * responsible for JWT validation and user identity extraction. This service therefore trusts the
 * {@code X-User-Id} header injected by the gateway rather than performing independent JWT
 * validation.
 *
 * <p>Authentication is delegated to {@link GatewayAuthenticationFilter}, which
 * reads the {@code X-User-Id} header and populates the Spring Security context.
 *
 * <p>Security model:
 * - CSRF disabled — stateless REST and WebSocket; no browser session involved - Session management
 * set to stateless — no HTTP session is created or used - Spring Security OAuth2 resource server
 * disabled — JWT validation is handled by the gateway; this service trusts the gateway exclusively
 *
 * <p>TODO: When the gateway is fully deployed, evaluate enabling secondary JWT
 * validation here as defence-in-depth, or document explicitly that gateway-only trust is the
 * intended architectural decision.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Configures the security filter chain for the chat service.
   *
   * <p>Permit rules:
   * - {@code /ws/**} — WebSocket handshake must be accessible before authentication
   * - {@code /actuator/health} — required for Docker and orchestrator health checks
   * - {@code /actuator/**} — open in development; restrict to internal network in production
   * - {@code /swagger-ui/**}, {@code /v3/api-docs/**} — open in development;
   *       disable via {@code springdoc.swagger-ui.enabled=false} in production profile
   * - All other requests — require authentication via {@code X-User-Id} header
   *
   * @param http the {@link HttpSecurity} builder provided by Spring Security
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if the security configuration cannot be applied
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(
            new GatewayAuthenticationFilter(),
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
  }
}