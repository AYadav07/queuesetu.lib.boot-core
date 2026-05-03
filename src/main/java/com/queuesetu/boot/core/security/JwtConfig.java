package com.queuesetu.boot.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    protected Key key;

    @PostConstruct
    protected void initKey() {
        // 1) Try Base64 (recommended for JWT secrets)
        // 2) Fall back to raw string bytes (useful for local/test)
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody();
    }

    public List<List<String>> getRolesFromClaims(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            List<?> outer = (List<?>) rolesObj;
            List<List<String>> result = new ArrayList<>();
            for (Object inner : outer) {
                if (inner instanceof List) {
                    List<?> innerList = (List<?>) inner;
                    List<String> pair = innerList.stream()
                            .map(o -> o == null ? "" : o.toString())
                            .collect(Collectors.toList());
                    result.add(pair);
                } else if (inner instanceof Object[]) {
                    Object[] arr = (Object[]) inner;
                    List<String> pair = Arrays.stream(arr).map(o -> o == null ? "" : o.toString()).collect(Collectors.toList());
                    result.add(pair);
                } else if (inner instanceof Map) {
                    // support an alternative structure {"role":..., "id":...}
                    Map<?, ?> m = (Map<?, ?>) inner;
                    Object rObj = m.get("role");
                    Object idObj = m.get("id");
                    String r = rObj == null ? "" : rObj.toString();
                    String id = idObj == null ? "" : idObj.toString();
                    result.add(Arrays.asList(r, id));
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}

