package com.construction.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SupabaseJwtAuthenticationConverter supabaseJwtAuthenticationConverter;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.supabase.url:https://placeholder.supabase.co}")
    private String supabaseUrl;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        // Public system endpoints
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Authentication endpoints require verified Supabase JWT
                        .requestMatchers("/api/auth/builder/**").authenticated()
                        .requestMatchers("/api/auth/contractor/**").authenticated()
                        .requestMatchers("/api/auth/client/**").authenticated()
                        // All project and operational APIs require authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(supabaseJwtAuthenticationConverter)
                        )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        // 1. Signature & asymmetric JWKS validation is handled by NimbusJwtDecoder
        // 2. Expiration and nbf timestamp validation
        validators.add(new JwtTimestampValidator());

        // 3. Audience validation ("authenticated" for Supabase user access tokens)
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audience = token.getAudience();
            if (audience != null && audience.contains("authenticated")) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "The token audience does not contain 'authenticated'", null)
            );
        };
        validators.add(audienceValidator);

        // 4. Issuer validation (if Supabase URL is real and not placeholder)
        if (supabaseUrl != null && !supabaseUrl.isBlank() && !supabaseUrl.contains("placeholder")) {
            String expectedIssuer = supabaseUrl.replaceAll("/+$", "") + "/auth/v1";
            validators.add(new JwtIssuerValidator(expectedIssuer));
        }

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return jwtDecoder;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("success", false);
            errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            errorDetails.put("error", "Unauthorized");
            errorDetails.put("message", "Full authentication is required to access this resource: " + authException.getMessage());
            errorDetails.put("path", request.getRequestURI());
            errorDetails.put("timestamp", LocalDateTime.now().toString());

            objectMapper.writeValue(response.getOutputStream(), errorDetails);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Explicit origin allowlist (disallows wildcard '*' when credentials are true)
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("*"))
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
