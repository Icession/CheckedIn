package edu.cit.checkedin.auth;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Starts the signed-in session after a successful sign-in.
 * Covers NFR-009: the browser gets an HttpOnly, Secure, SameSite session cookie that expires
 * after 8 hours (the cookie settings are in application.properties).
 */
@Service
public class SessionService {

    // Stores "who is signed in" inside the server-side session that the cookie points to
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /** Signs the user in: starts a fresh session and remembers their verified email in it. */
    public void startSession(String verifiedEmail, HttpServletRequest request, HttpServletResponse response) {
        // Throw away any older session first, so nobody can plant a session ID before sign-in
        // and use it afterwards (an attack called "session fixation")
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        request.getSession(true);

        // Step 1g adds the user's role here
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(verifiedEmail, null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Spring Security does not save a sign-in made in our own code by itself, so we save it here
        securityContextRepository.saveContext(context, request, response);
    }
}
