package edu.cit.checkedin.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.auth.dto.SaveNameRequest;
import edu.cit.checkedin.password.PasswordRules;

/**
 * Turns a proven @cit.edu email into a CheckedIn account.
 * Covers FR-001 (one person may use both sign-in methods: one account, found by email),
 * FR-003 (the account is created only after the user enters their name),
 * FR-004 (role from the professor list; the stored role is kept in step with it) and
 * FR-005 – FR-006 (a student links one student ID, which no other account may use).
 * Increment 2 / SRS v1.4: the first password is created together with the account, and is saved only as a hash.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginCodeService loginCodeService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(UserRepository userRepository, LoginCodeService loginCodeService,
                       RoleService roleService, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.loginCodeService = loginCodeService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
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

    /** Password sign-in, after PasswordSignInService checked the password (Increment 2 / SRS v1.4). */
    @Transactional
    public CurrentUserResponse signInWithPassword(String verifiedEmail) {
        return recordSignIn(verifiedEmail, null);
    }

    /**
     * Who is signed in, for GET /api/auth/me. The role comes from the professor list right now
     * (FR-008); if it differs from the stored one, the stored role is corrected.
     */
    public CurrentUserResponse getCurrentUser(String verifiedEmail) {
        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        if (existingUser.isEmpty()) {
            return CurrentUserResponse.withoutAccount(verifiedEmail);
        }
        User user = existingUser.get();
        Role currentRole = assignRole(verifiedEmail);
        if (user.getRole() != currentRole) {
            user.setRole(currentRole);
            userRepository.save(user);
        }
        return CurrentUserResponse.from(user);
    }

    /** The signed-in person's account, for pages that need one (for example the dashboards). */
    public User requireAccount(String verifiedEmail) {
        return userRepository.findByEmail(verifiedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please finish signing in first: enter your name."));
    }

    /** FR-005: student pages need a linked student ID, even if someone types their address directly. */
    public User requireStudentWithId(String verifiedEmail) {
        User student = requireAccount(verifiedEmail);
        if (student.getStudentNumber() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please link your student ID first.");
        }
        return student;
    }

    /**
     * FR-003: creates the account once the user has entered their name (and, since Increment 2, their first
     * password). Until then no row exists, so someone who closes the page before this step just sees the name
     * form again next time.
     */
    @Transactional
    public CurrentUserResponse saveName(String verifiedEmail, Long githubId, SaveNameRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        if (existingUser.isPresent()) {
            // Already created (for example by a double click): keep it as it is
            return CurrentUserResponse.from(existingUser.get());
        }
        PasswordRules.checkNotEmailAndConfirmed(request.password(), request.confirmPassword(), verifiedEmail);
        User user = new User(verifiedEmail, request.firstName(), request.lastName(), assignRole(verifiedEmail));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastLoginAt(LocalDateTime.now(clock));
        if (githubId != null) {
            linkGitHubAccount(user, githubId);
        }
        return CurrentUserResponse.from(userRepository.save(user));
    }

    /**
     * FR-005, FR-006: saves the student ID a student enters once; one ID belongs to one account.
     * Linking the student's enrollments and the last-name check (rest of FR-006, FR-007) are added
     * in Phase 4, when class lists exist.
     */
    public CurrentUserResponse linkStudentNumber(String verifiedEmail, String studentNumber) {
        User user = userRepository.findByEmail(verifiedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your name first."));
        // The professor list right now decides, like every other role check (FR-008)
        if (assignRole(verifiedEmail) != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students link a student ID.");
        }
        if (user.getStudentNumber() != null) {
            return keepLinkedStudentNumber(user, studentNumber);
        }
        if (userRepository.existsByStudentNumber(studentNumber)) {
            throw studentIdAlreadyTaken();
        }
        user.linkStudentNumber(studentNumber);
        try {
            return CurrentUserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // Someone linked the same ID at the very same moment; the database's unique rule caught it
            throw studentIdAlreadyTaken();
        }
    }

    // Once linked, a student cannot change their ID themselves (there is no admin page)
    private CurrentUserResponse keepLinkedStudentNumber(User user, String studentNumber) {
        if (user.getStudentNumber().equals(studentNumber)) {
            // The same ID sent again (for example a double click): nothing to change
            return CurrentUserResponse.from(user);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Your account is already linked to student ID "
                + user.getStudentNumber() + ". If that is wrong, please contact your professor.");
    }

    private ResponseStatusException studentIdAlreadyTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "This student ID is already linked to another account. If it is yours, please contact your professor.");
    }

    /** FR-004: Professor if the email is on the professor list in the settings, otherwise Student. */
    public Role assignRole(String verifiedEmail) {
        return roleService.roleFor(verifiedEmail);
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
