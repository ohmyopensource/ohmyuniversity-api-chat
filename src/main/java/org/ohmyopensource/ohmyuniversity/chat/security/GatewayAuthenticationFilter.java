package org.ohmyopensource.ohmyuniversity.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the X-User-Id header injected by the API gateway after JWT validation
 * and populates the Spring Security context with a simple authentication token.
 *
 * // @TODO: When the gateway is ready and this service is exposed only internally
 * // (e.g. via Docker network), verify that X-User-Id cannot be spoofed by
 * // ensuring the service is truly unreachable from outside the internal network.
 * // If extra hardening is needed, validate a shared internal secret header
 * // (e.g. X-Internal-Secret) alongside X-User-Id.
 */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

  private static final String USER_ID_HEADER = "X-User-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String userId = request.getHeader(USER_ID_HEADER);

    if (userId != null && !userId.isBlank()) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userId,
              null,
              List.of(new SimpleGrantedAuthority("ROLE_USER"))
          );
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}