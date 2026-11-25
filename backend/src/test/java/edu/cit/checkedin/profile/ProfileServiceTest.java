package edu.cit.checkedin.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.profile.dto.ProfileResponse;
import edu.cit.checkedin.profile.dto.UpdateProfileRequest;

/** Tests the profile rules (Increment 2 / SRS v1.4) with a fake database. */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final String STUDENT_EMAIL = "juan.delacruz@cit.edu";

    @Mock
    private AuthService authService;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ProfileService service;

    @Test
    void showsEmailNamesRoleStudentIdAndTheDayTheAccountWasCreated() {
        when(authService.requireAccount(STUDENT_EMAIL)).thenReturn(juan());
        when(authService.assignRole(STUDENT_EMAIL)).thenReturn(Role.STUDENT);

        ProfileResponse profile = service.getProfile(STUDENT_EMAIL);

        assertThat(profile).isEqualTo(new ProfileResponse(STUDENT_EMAIL, "Juan", "Cruz", Role.STUDENT,
                "22-1234-567", LocalDate.of(2026, 11, 10)));
    }

    @Test
    void theRoleComesFromTheProfessorListRightNow() {
        when(authService.requireAccount(STUDENT_EMAIL)).thenReturn(juan()); // stored as a student
        when(authService.assignRole(STUDENT_EMAIL)).thenReturn(Role.PROFESSOR); // the list now says professor

        assertThat(service.getProfile(STUDENT_EMAIL).role()).isEqualTo(Role.PROFESSOR);
    }

    @Test
    void savesTheNewNamesButNeverTheEmailRoleOrStudentId() {
        User juan = juan();
        when(authService.requireAccount(STUDENT_EMAIL)).thenReturn(juan);
        when(authService.assignRole(STUDENT_EMAIL)).thenReturn(Role.STUDENT);
        when(userRepository.save(juan)).thenReturn(juan);

        ProfileResponse profile = service.updateName(STUDENT_EMAIL, new UpdateProfileRequest(" Ma.  Cristina ", "Dela Cruz"));

        assertThat(profile.firstName()).isEqualTo("Ma. Cristina");
        assertThat(profile.lastName()).isEqualTo("Dela Cruz");
        assertThat(juan.getEmail()).isEqualTo(STUDENT_EMAIL);
        assertThat(juan.getStudentNumber()).isEqualTo("22-1234-567");
        assertThat(juan.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void someoneWhoHasNotFinishedSigningInHasNoProfileYet() {
        when(authService.requireAccount(STUDENT_EMAIL)).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please finish signing in first: enter your name."));

        assertThatThrownBy(() -> service.updateName(STUDENT_EMAIL, new UpdateProfileRequest("Juan", "Cruz")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Please finish signing in first");
        verify(userRepository, never()).save(any());
    }

    // A student account created on 10 November 2026 at 9:30 PM (Philippine time)
    private static User juan() {
        User user = new User(STUDENT_EMAIL, "Juan", "Cruz", Role.STUDENT);
        user.linkStudentNumber("22-1234-567");
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 11, 10, 21, 30));
        return user;
    }
}
