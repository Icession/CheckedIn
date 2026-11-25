package edu.cit.checkedin.common;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cit.checkedin.auth.GitHubLoginHandler;
import edu.cit.checkedin.auth.GitHubUserService;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.RoleService;

/**
 * Who may call which URL, and how signed-in sessions work.
 * Covers FR-008 (professor pages only for professors, student pages only for students, otherwise
 * HTTP 403), NFR-008 (GitHub sign-in on the server), NFR-009 (session cookie) and the base of
 * NFR-010 (every endpoint needs a signed-in user unless it is listed as public below).
 */
@Configuration
@EnableWebSecurity
// These rules are for web requests only. Without this line the test-email command (which starts
// the app without a web server, see README) would fail looking for GitHub's web sign-in setup.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    // Endpoints anyone may call without being signed in (signing out twice is harmless)
    private static final String[] PUBLIC_URLS = {
        "/api/health", "/api/auth/code", "/api/auth/code/verify", "/api/auth/sign-out", "/error",
        // Increment 2 / SRS v1.4: password sign-in
        "/api/auth/password/sign-in"
    };

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GitHubUserService gitHubUserService,
                                                   GitHubLoginHandler gitHubLoginHandler,
                                                   RoleService roleService) throws Exception {
        http
                // CSRF tokens are turned off on purpose: the session cookie is SameSite=Lax, so other
                // websites cannot send it with a POST, and our API accepts only JSON. If the frontend
                // and backend end up on different sites when deployed (Phase 12), revisit this.
                .csrf(AbstractHttpConfigurer::disable)
                // We have our own sign-in, so turn off Spring's built-in login page and pop-up
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Spring's own sign-out is off too: we sign out with POST /api/auth/sign-out (AuthController)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // FR-008: professor pages only for professors, student pages only for students
                        .requestMatchers("/api/professor/**").access(hasRoleRightNow(roleService, Role.PROFESSOR))
                        .requestMatchers("/api/student/**").access(hasRoleRightNow(roleService, Role.STUDENT))
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
                        // Nobody is signed in
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Please sign in first."))
                        // Signed in, but with the other role (FR-008)
                        .accessDeniedHandler((request, response, exception) ->
                                writeJsonError(response, HttpStatus.FORBIDDEN, "Sorry, you can't open this page. "
                                        + "Professor pages are only for professors, and student pages only for students.")));
        return http.build();
    }

    /**
     * Allows the request only if someone is signed in AND the professor list says, right now, that
     * they have this role. Asked on every request (the list is in memory, so this is cheap), so a
     * change to the list takes effect at once instead of at the next sign-in.
     */
    private static AuthorizationManager<RequestAuthorizationContext> hasRoleRightNow(RoleService roleService, Role role) {
        return (authentication, requestContext) -> {
            Authentication signIn = authentication.get();
            boolean signedIn = signIn != null && signIn.isAuthenticated()
                    && !(signIn instanceof AnonymousAuthenticationToken);
            return new AuthorizationDecision(signedIn && roleService.roleFor(signIn.getName()) == role);
        };
    }

    // Spring Security stops a request before it reaches our controllers, so GlobalExceptionHandler
    // never sees these errors. This writes the same { "message": "..." } JSON by hand.
    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }
}
