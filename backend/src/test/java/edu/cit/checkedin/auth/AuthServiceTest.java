package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.auth.dto.SaveNameRequest;

/**
 * Tests account creation, roles and the student ID (FR-001, FR-003 – FR-006) with a fake database.
 * The professor list in these tests holds one email: maria.santos@cit.edu.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 8, 0);
    private static final String PROFESSOR_EMAIL = "maria.santos@cit.edu";
    private static final String STUDENT_EMAIL = "juan.delacruz@cit.edu";
    private static final Long GITHUB_ID = 583231L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginCodeService loginCodeService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        // Written messily on purpose (spaces, capitals, an empty entry), like a hand-edited .env line
        List<String> professorList = List.of("  Maria.Santos@CIT.edu ", "");
        service = new AuthService(userRepository, loginCodeService, new EmailAddressChecker(List.of("cit.edu")),
                professorList, Clock.fixed(NOW.atZone(MANILA).toInstant(), MANILA));
    }

    // ---------- Rule 1: role from the professor list ----------

    @Test
    void professorListIgnoresCapitalsSpacesAndEmptyEntries() {
        assertThat(service.assignRole(PROFESSOR_EMAIL)).isEqualTo(Role.PROFESSOR);
        assertThat(service.assignRole(STUDENT_EMAIL)).isEqualTo(Role.STUDENT);
        assertThat(service.assignRole("")).isEqualTo(Role.STUDENT);
    }

    @Test
    void someoneAddedToTheListBecomesAProfessorAtTheirNextSignIn() {
        User user = existingUser(PROFESSOR_EMAIL, Role.STUDENT);
        signsInWithCode(PROFESSOR_EMAIL, user);

        CurrentUserResponse result = service.signInWithCode(PROFESSOR_EMAIL, "123456");

        assertThat(result.role()).isEqualTo(Role.PROFESSOR);
        assertThat(result.needsName()).isFalse();
        assertThat(user.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void someoneRemovedFromTheListBecomesAStudentAtTheirNextSignIn() {
        User user = existingUser(STUDENT_EMAIL, Role.PROFESSOR);
        signsInWithCode(STUDENT_EMAIL, user);

        assertThat(service.signInWithCode(STUDENT_EMAIL, "123456").role()).isEqualTo(Role.STUDENT);
    }

    // ---------- Rule 4: no account until the name is entered ----------

    @Test
    void aFirstSignInCreatesNoAccountAndAsksForTheName() {
        when(loginCodeService.checkCode(STUDENT_EMAIL, "123456")).thenReturn(STUDENT_EMAIL);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());

        CurrentUserResponse result = service.signInWithCode(STUDENT_EMAIL, "123456");

        assertThat(result.needsName()).isTrue();
        assertThat(result.email()).isEqualTo(STUDENT_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    void savingTheNameCreatesTheAccountWithItsRoleAndGitHubId() {
        when(userRepository.findByEmail(PROFESSOR_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByGithubId(GITHUB_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        CurrentUserResponse result = service.saveName(PROFESSOR_EMAIL, GITHUB_ID, new SaveNameRequest("Maria", "Dela Cruz"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getFirstName()).isEqualTo("Maria");
        assertThat(savedUser.getValue().getLastName()).isEqualTo("Dela Cruz");
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.PROFESSOR);
        assertThat(savedUser.getValue().getGithubId()).isEqualTo(GITHUB_ID);
        assertThat(savedUser.getValue().getLastLoginAt()).isEqualTo(NOW);
        assertThat(result.needsName()).isFalse();
    }

    @Test
    void anEmailCodeUserGetsNoGitHubIdAndIsAStudentIfNotOnTheList() {
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        CurrentUserResponse result = service.saveName(STUDENT_EMAIL, null, new SaveNameRequest("Juan", "Dela Cruz"));

        assertThat(result.role()).isEqualTo(Role.STUDENT);
        verify(userRepository, never()).findByGithubId(any());
    }

    @Test
    void savingTheNameAgainDoesNotCreateASecondAccount() {
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(existingUser(STUDENT_EMAIL, Role.STUDENT)));

        service.saveName(STUDENT_EMAIL, null, new SaveNameRequest("Juan", "Dela Cruz"));

        verify(userRepository, never()).save(any());
    }

    // ---------- Rule 3: same person, both methods ----------

    @Test
    void gitHubAfterTheEmailCodeSavesTheGitHubIdOnTheSameAccount() {
        User user = existingUser(STUDENT_EMAIL, Role.STUDENT);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByGithubId(GITHUB_ID)).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        CurrentUserResponse result = service.signInWithGitHub(GITHUB_ID, STUDENT_EMAIL);

        assertThat(user.getGithubId()).isEqualTo(GITHUB_ID);
        assertThat(result.needsName()).isFalse();
        // Only the existing account was saved: no second account
        ArgumentCaptor<User> savedUsers = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUsers.capture());
        assertThat(savedUsers.getAllValues()).containsExactly(user);
    }

    @Test
    void aGitHubAccountUsedByAnotherAccountBeforeIsMovedWithoutBreakingTheUniqueRule() {
        User oldAccount = existingUser("old.address@cit.edu", Role.STUDENT);
        ReflectionTestUtils.setField(oldAccount, "userId", 1L);
        oldAccount.setGithubId(GITHUB_ID);
        User user = existingUser(STUDENT_EMAIL, Role.STUDENT);
        ReflectionTestUtils.setField(user, "userId", 2L);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByGithubId(GITHUB_ID)).thenReturn(Optional.of(oldAccount));
        when(userRepository.save(user)).thenReturn(user);

        service.signInWithGitHub(GITHUB_ID, STUDENT_EMAIL);

        assertThat(oldAccount.getGithubId()).isNull();
        assertThat(user.getGithubId()).isEqualTo(GITHUB_ID);
        // The old account is written first, so the ID is never on two accounts at once
        InOrder order = inOrder(userRepository);
        order.verify(userRepository).saveAndFlush(oldAccount);
        order.verify(userRepository).save(user);
    }

    // ---------- Student ID (FR-005, FR-006) ----------

    @Test
    void onlyANewStudentAccountAsksForTheStudentId() {
        assertThat(CurrentUserResponse.from(existingUser(STUDENT_EMAIL, Role.STUDENT)).needsStudentId()).isTrue();
        assertThat(CurrentUserResponse.from(existingUser(PROFESSOR_EMAIL, Role.PROFESSOR)).needsStudentId()).isFalse();
    }

    @Test
    void aStudentLinksTheirStudentId() {
        User student = existingUser(STUDENT_EMAIL, Role.STUDENT);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(userRepository.existsByStudentNumber("12-3456-789")).thenReturn(false);
        when(userRepository.saveAndFlush(student)).thenReturn(student);

        CurrentUserResponse result = service.linkStudentNumber(STUDENT_EMAIL, "12-3456-789");

        assertThat(student.getStudentNumber()).isEqualTo("12-3456-789");
        assertThat(result.studentNumber()).isEqualTo("12-3456-789");
        assertThat(result.needsStudentId()).isFalse();
    }

    @Test
    void anIdAlreadyLinkedToAnotherAccountIsRefused() {
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(existingUser(STUDENT_EMAIL, Role.STUDENT)));
        when(userRepository.existsByStudentNumber("12-3456-789")).thenReturn(true);

        assertRejected(() -> service.linkStudentNumber(STUDENT_EMAIL, "12-3456-789"),
                HttpStatus.CONFLICT, "already linked to another account");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void twoStudentsLinkingTheSameIdAtTheSameMomentGetTheSameClearMessage() {
        User student = existingUser(STUDENT_EMAIL, Role.STUDENT);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(userRepository.existsByStudentNumber("12-3456-789")).thenReturn(false);
        // The other student saved first, so the database's unique rule refuses this save
        when(userRepository.saveAndFlush(student)).thenThrow(new DataIntegrityViolationException("unique student_number"));

        assertRejected(() -> service.linkStudentNumber(STUDENT_EMAIL, "12-3456-789"),
                HttpStatus.CONFLICT, "already linked to another account");
    }

    @Test
    void aLinkedIdCannotBeChangedButSendingTheSameIdAgainIsFine() {
        User student = existingUser(STUDENT_EMAIL, Role.STUDENT);
        student.linkStudentNumber("12-3456-789");
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));

        assertThat(service.linkStudentNumber(STUDENT_EMAIL, "12-3456-789").studentNumber()).isEqualTo("12-3456-789");
        assertRejected(() -> service.linkStudentNumber(STUDENT_EMAIL, "98-7654-321"),
                HttpStatus.CONFLICT, "already linked to student ID 12-3456-789");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void professorsDoNotLinkAStudentId() {
        when(userRepository.findByEmail(PROFESSOR_EMAIL)).thenReturn(Optional.of(existingUser(PROFESSOR_EMAIL, Role.PROFESSOR)));

        assertRejected(() -> service.linkStudentNumber(PROFESSOR_EMAIL, "12-3456-789"),
                HttpStatus.FORBIDDEN, "Only students");
    }

    @Test
    void someoneWithoutAnAccountMustEnterTheirNameFirst() {
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());

        assertRejected(() -> service.linkStudentNumber(STUDENT_EMAIL, "12-3456-789"),
                HttpStatus.BAD_REQUEST, "name first");
    }

    // ---------- helpers ----------

    private static void assertRejected(ThrowingCallable call, HttpStatus status, String messagePart) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(status);
            assertThat(exception.getReason()).contains(messagePart);
        });
    }

    private User existingUser(String email, Role role) {
        return new User(email, "Test", "User", role);
    }

    private void signsInWithCode(String email, User user) {
        when(loginCodeService.checkCode(email, "123456")).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
    }
}
