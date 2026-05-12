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
 * Spring Security configuration for the chat service.
 *
 * // @TODO: When the gateway is deployed and all traffic is routed through it,
 * // review whether to add secondary JWT validation here as defence-in-depth,
 * // or keep trusting the gateway exclusively (simpler, standard microservice pattern).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF disabled — stateless REST + WebSocket, no browser session
        .csrf(AbstractHttpConfigurer::disable)

        // Stateless — no HTTP session, authentication is per-request via header
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Disable Spring Security's default OAuth2 resource server auto-config.
        // @TODO: When the gateway is ready, evaluate enabling JWT validation here
        // as an additional security layer:
        //   .oauth2ResourceServer(oauth2 -> oauth2
        //       .jwt(jwt -> jwt.decoder(jwtDecoder())))
        .oauth2ResourceServer(AbstractHttpConfigurer::disable)

        .authorizeHttpRequests(auth -> auth
            // WebSocket handshake endpoint — must be accessible before authentication
            // @TODO: Restrict this further if the service is exposed beyond the
            // internal Docker network (e.g. add IP filtering at gateway level)
            .requestMatchers("/ws/**").permitAll()

            // Actuator health — open for Docker/ECS health checks
            // @TODO: In production, expose only /actuator/health and restrict
            // /actuator/prometheus to the internal observability network
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/**").permitAll()

            // Swagger UI — open in dev, should be disabled in prod profile
            // @TODO: Restrict swagger to dev profile only via
            // @Profile("dev") on SpringDocConfig or springdoc.swagger-ui.enabled=false
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

            // All other endpoints require authentication via X-User-Id header
            .anyRequest().authenticated()
        )

        // Register our gateway trust filter before Spring's default auth filter
        .addFilterBefore(
            new GatewayAuthenticationFilter(),
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
  }
}