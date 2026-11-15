package edu.cit.checkedin.auth;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Starts and ends the signed-in session.
 * Covers NFR-009 (the browser gets an HttpOnly, Secure, SameSite session cookie that expires after
 * 8 hours; the cookie settings are in application.properties) and FR-009 (sign out).
 */
@Service
public class SessionService {

    // Where a GitHub sign-in keeps the GitHub account ID until the account is created (name form)
    public static final String GITHUB_ID_SESSION_KEY = "checkedin.githubId";

    // Stores "who is signed in" inside the server-side session that the cookie points to
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final String cookieName;
    private final boolean cookieSecure;

    public SessionService(@Value("${server.servlet.session.cookie.name}") String cookieName,
                          @Value("${server.servlet.session.cookie.secure}") boolean cookieSecure) {
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    /** Signs the user in: starts a fresh session and remembers their verified email in it. */
    public void startSession(String verifiedEmail, HttpServletRequest request, HttpServletResponse response) {
        // Throw away any older session first, so nobody can plant a session ID before sign-in
        // and use it afterwards (an attack called "session fixation")
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        request.getSession(true);

        // Only the email is stored. The role is NOT: SecurityConfig asks RoleService on every
        // request, so a change to the professor list applies right away (FR-008).
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(verifiedEmail, null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Spring Security does not save a sign-in made in our own code by itself, so we save it here
        securityContextRepository.saveContext(context, request, response);
    }

    /**
     * Signs the user out (FR-009): ends the server-side session and tells the browser to delete the
     * cookie. It does not sign the user out of GitHub, which is why the page shows a hint about that.
     */
    public void endSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        // Same name, path and Secure setting as the session cookie, but already expired (Max-Age=0),
        // so the browser deletes it
        ResponseCookie expiredCookie = ResponseCookie.from(cookieName, "")
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }

    /** Remembers which GitHub account was used, so it can be saved when the account is created. */
    public void rememberGitHubId(Long githubId, HttpServletRequest request) {
        request.getSession().setAttribute(GITHUB_ID_SESSION_KEY, githubId);
    }

    /**
     * The GitHub ID remembered at sign-in, or null after an email-code sign-in. (Every sign-in
     * starts a fresh session, so an email-code session never holds an old GitHub ID.)
     */
    public Long getRememberedGitHubId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session == null) ? null : (Long) session.getAttribute(GITHUB_ID_SESSION_KEY);
    }
}
