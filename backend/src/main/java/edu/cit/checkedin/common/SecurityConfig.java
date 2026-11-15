package edu.cit.checkedin.common;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cit.checkedin.auth.GitHubLoginHandler;
import edu.cit.checkedin.auth.GitHubUserService;

/**
 * Who may call which URL, and how signed-in sessions work.
 * Covers NFR-008 (GitHub sign-in on the server), NFR-009 (session cookie) and the base of NFR-010
 * (every endpoint needs a signed-in user unless it is listed as public below).
 * Role checks are added in step 1i.
 */
@Configuration
@EnableWebSecurity
// These rules are for web requests only. Without this line the test-email command (which starts
// the app without a web server, see README) would fail looking for GitHub's web sign-in setup.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GitHubUserService gitHubUserService,
                                                   GitHubLoginHandler gitHubLoginHandler) throws Exception {
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
                // "Sign in with GitHub" (FR-001, NFR-008). Spring Security handles the OAuth steps:
                //   /oauth2/authorization/github  -> sends the browser to GitHub with a random "state"
                //   /login/oauth2/code/github     -> checks the state, then swaps GitHub's one-time code
                //                                    for an access token on the server (secret stays here)
                .oauth2Login(oauth -> oauth
                        // Our check: the account must have a verified @cit.edu email (FR-002)
                        .userInfoEndpoint(userInfo -> userInfo.userService(gitHubUserService))
                        // Keep GitHub's token in the session only; starting our session throws it away
                        .authorizedClientRepository(new HttpSessionOAuth2AuthorizedClientRepository())
                        .successHandler(gitHubLoginHandler)
                        .failureHandler(gitHubLoginHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Please sign in first.")));
        return http.build();
    }

    // Spring Security stops a request before it reaches our controllers, so GlobalExceptionHandler
    // never sees these errors. This writes the same { "message": "..." } JSON by hand.
    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }
}
