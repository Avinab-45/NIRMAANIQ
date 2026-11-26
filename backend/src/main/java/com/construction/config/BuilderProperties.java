package com.construction.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Validates and exposes Builder security configuration properties.
 * Enforces mandatory supply of BUILDER_ACTIVATION_CODE with zero insecure fallback defaults.
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.builder")
@Getter
@Setter
public class BuilderProperties {

    /**
     * Secret activation code required for activating a Builder account.
     * Must be explicitly provided via the BUILDER_ACTIVATION_CODE environment variable.
     */
    private String activationCode;

    @PostConstruct
    public void validate() {
        if (activationCode == null || activationCode.trim().isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY CONFIGURATION ERROR: 'BUILDER_ACTIVATION_CODE' is not configured! " +
                    "You must supply a non-blank BUILDER_ACTIVATION_CODE environment variable. " +
                    "Application startup aborted to prevent insecure fallback."
            );
        }
        log.info("Builder activation code successfully configured and validated (value masked for security).");
    }
}
