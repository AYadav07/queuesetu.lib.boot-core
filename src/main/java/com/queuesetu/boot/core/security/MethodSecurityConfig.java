package com.queuesetu.boot.core.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Auto-configuration that enables Spring Security method-level security
 * ({@code @PreAuthorize}, {@code @PostAuthorize}, etc.) and registers the
 * {@link RbacGuard} bean ({@code @rbac}) used by BFF controller annotations.
 *
 * <p>This is registered in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so every service that depends on {@code boot-core} inherits it automatically.
 */
@AutoConfiguration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public RbacGuard rbacGuard() {
        return new RbacGuard();
    }
}
