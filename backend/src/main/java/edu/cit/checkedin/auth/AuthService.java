package edu.cit.checkedin.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.auth.dto.SaveNameRequest;

/**
 * Turns a proven @cit.edu email into a CheckedIn account.
 * Covers FR-001 (one person may use both sign-in methods: one account, found by email),
 * FR-003 (the account is created only after the user enters their name) and
 * FR-004 (role from the professor list, checked again at every sign-in).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginCodeService loginCodeService;
    private final Set<String> professorEmails;
    private final Clock clock;

    public AuthService(UserRepository userRepository, LoginCodeService loginCodeService,
                       EmailAddressChecker emailAddressChecker,
                       @Value("${checkedin.auth.professor-emails}") List<String> professorEmails, Clock clock) {
        this.userRepository = userRepository;
        this.loginCodeService = loginCodeService;
        // Cleaned up like every other email (lowercase, no spaces); empty entries are ignored
        this.professorEmails = professorEmails.stream()
                .map(emailAddressChecker::normalize)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toSet());
        this.clock = clock;
    }

    /**
     * Email-code sign-in. Deliberately NOT @Transactional: checkCode must keep a wrong try saved
     * even though it then throws an error, and a surrounding transaction would undo that save.
     */
    public CurrentUserResponse signInWithCode(String email, String code) {
        String verifiedEmail = loginCodeService.checkCode(email, code);
        return recordSignIn(verifiedEmail, null);
    }

    /**
     * GitHub sign-in, after GitHubUserService found the verified @cit.edu email. If the person
     * already has an account (for example from the email code), the GitHub ID is saved on it.
     */
    @Transactional
    public CurrentUserResponse signInWithGitHub(Long githubId, String verifiedEmail) {
        return recordSignIn(verifiedEmail, githubId);
    }

    /** Who is signed in, for GET /api/auth/me. */
    public CurrentUserResponse getCurrentUser(String verifiedEmail) {
        return userRepository.findByEmail(verifiedEmail)
                .map(CurrentUserResponse::from)
                .orElseGet(() -> CurrentUserResponse.withoutAccount(verifiedEmail));
    }

    /**
     * FR-003: creates the account once the user has entered their name. Until then no row exists,
     * so someone who closes the page before this step just sees the name form again next time.
     */
    @Transactional
    public CurrentUserResponse saveName(String verifiedEmail, Long githubId, SaveNameRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        if (existingUser.isPresent()) {
            // Already created (for example by a double click): keep it as it is
            return CurrentUserResponse.from(existingUser.get());
        }
        User user = new User(verifiedEmail, request.firstName(), request.lastName(), assignRole(verifiedEmail));
        user.setLastLoginAt(LocalDateTime.now(clock));
        if (githubId != null) {
            linkGitHubAccount(user, githubId);
        }
        return CurrentUserResponse.from(userRepository.save(user));
    }

    /** FR-004: Professor if the email is on the professor list in the settings, otherwise Student. */
    public Role assignRole(String verifiedEmail) {
        return professorEmails.contains(verifiedEmail) ? Role.PROFESSOR : Role.STUDENT;
    }

    // For an existing account: check the role again (the professor list may have changed), note the
    // sign-in time, and remember the GitHub account if GitHub was used. Creates nothing when there is
    // no account yet, because the name has to come first (FR-003).
    private CurrentUserResponse recordSignIn(String verifiedEmail, Long githubId) {
        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        if (existingUser.isEmpty()) {
            return CurrentUserResponse.withoutAccount(verifiedEmail);
        }
        User user = existingUser.get();
        user.setRole(assignRole(verifiedEmail));
        user.setLastLoginAt(LocalDateTime.now(clock));
        if (githubId != null) {
            linkGitHubAccount(user, githubId);
        }
        return CurrentUserResponse.from(userRepository.save(user));
    }

    // A GitHub account belongs to one CheckedIn account (github_id is unique). If it was linked to
    // another account before (its owner now proves a different @cit.edu email), unlink it there first.
    private void linkGitHubAccount(User user, Long githubId) {
        if (githubId.equals(user.getGithubId())) {
            return;
        }
        userRepository.findByGithubId(githubId)
                .filter(otherUser -> !otherUser.getUserId().equals(user.getUserId()))
                .ifPresent(otherUser -> {
                    otherUser.setGithubId(null);
                    // Written to the database right away, before this account, so the unique rule holds
                    userRepository.saveAndFlush(otherUser);
                });
        user.setGithubId(githubId);
    }
}
