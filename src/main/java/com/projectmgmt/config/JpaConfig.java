package com.projectmgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.projectmgmt.domain")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.empty();
            }
            if (auth.getPrincipal() instanceof com.projectmgmt.domain.user.User user) {
                return Optional.of(user.getId());
            }
            return Optional.empty();
        };
    }
}
