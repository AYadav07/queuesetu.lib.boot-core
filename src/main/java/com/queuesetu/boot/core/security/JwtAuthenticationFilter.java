package com.queuesetu.boot.core.security;

import com.queuesetu.boot.core.user.UserDetail;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtConfig jwtConfig;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            Claims claims = jwtConfig.validateTokenAndGetClaims(jwt);


            String email = claims.getSubject();

            List<List<String>> rolesPairs = jwtConfig.getRolesFromClaims(claims);

            // Build authorities from the roles pairs (use role string as authority). Fall back to single "role" claim.
            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            if (rolesPairs != null && !rolesPairs.isEmpty()) {
                for (List<String> pair : rolesPairs) {
                    if (pair != null && !pair.isEmpty()) {
                        String r = pair.getFirst();
                        if (r != null && !r.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority(r));
                        }
                    }
                }
            }
            if (authorities.isEmpty()) {
                String roleName = claims.get("role", String.class);
                if (roleName != null && !roleName.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(roleName));
                }
            }

            // Parse IDs safely (claims may store UUIDs or strings)
            UUID userId = null;
            try {
                Object uObj = claims.get("userId");
                if (uObj != null) userId = UUID.fromString(uObj.toString());
            } catch (IllegalArgumentException ignored) {
                // if any UUID parsing fails, leave that value as null
            }

            UserDetail userDetails = new UserDetail(email, "", authorities, userId, email, rolesPairs);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            // log and continue filter chain; invalid token should not break request processing
            logger.debug("JWT processing failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}