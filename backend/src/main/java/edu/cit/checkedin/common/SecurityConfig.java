package edu.cit.checkedin.common;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Who may call which URL, and how signed-in sessions work.
 * Covers NFR-009 (session cookie) and the base of NFR-010 (every endpoint needs a signed-in
 * user unless it is listed as public below). Role checks are added in step 1i.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Endpoints anyone may call without being signed in
    private static final String[] PUBLIC_URLS = {
        "/api/health", "/api/auth/code", "/api/auth/code/verify", "/error"
    };

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF tokens are turned off on purpose: the session cookie is SameSite=Lax, so other
                // websites cannot send it with a POST, and our API accepts only JSON. If the frontend
                // and backend end up on different sites when deployed (Phase 12), revisit this.
                .csrf(AbstractHttpConfigurer::disable)
                // We have our own sign-in, so turn off Spring's built-in login page and pop-up
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Sign-out is added in step 1i
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Please sign in first.")));
        return http.build();
    }

    /** BCrypt: a slow, salted hash, used for the emailed sign-in codes (NFR-008). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security stops a request before it reaches our controllers, so GlobalExceptionHandler
    // never sees these errors. This writes the same { "message": "..." } JSON by hand.
    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }
}
