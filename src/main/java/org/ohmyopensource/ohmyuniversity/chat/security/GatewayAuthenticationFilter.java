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
 * Spring Security filter that establishes the authentication context from the {@code X-User-Id}
 * header injected by the API gateway.
 *
 * <p>The OhMyUniversity API gateway validates the client JWT and extracts the
 * user identifier before forwarding requests to downstream services. This filter trusts that header
 * and uses it to populate the Spring Security context, avoiding redundant JWT validation at the
 * service level.
 *
 * <p>If the {@code X-User-Id} header is absent or blank, no authentication is
 * set and the request proceeds unauthenticated — the security filter chain will reject it if the
 * endpoint requires authentication.
 *
 * <p>Security note: this trust model is safe as long as the chat service is
 * reachable exclusively through the internal Docker network and not directly from external clients.
 * If additional hardening is required, consider validating a shared internal secret header (e.g.
 * {@code X-Internal-Secret}) alongside {@code X-User-Id}.
 */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

  /**
   * Name of the HTTP header carrying the authenticated user identifier, injected by the API gateway
   * after JWT validation.
   */
  private static final String USER_ID_HEADER = "X-User-Id";

  // ============ Override Methods ============

  /**
   * Reads the {@code X-User-Id} header and, if present and non-blank, populates the Spring Security
   * context with a {@link UsernamePasswordAuthenticationToken} granting {@code ROLE_USER}.
   *
   * @param request     the incoming HTTP request
   * @param response    the HTTP response
   * @param filterChain the filter chain to continue after processing
   * @throws ServletException if a servlet error occurs
   * @throws IOException      if an I/O error occurs
   */
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