package edu.cit.checkedin.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.profile.dto.ProfileResponse;
import edu.cit.checkedin.profile.dto.UpdateProfileRequest;

/**
 * The signed-in user's own profile (Increment 2 / SRS v1.4: view and edit profile).
 * Every method receives the email of the person signed in on this session (from the controller), never an
 * ID chosen by the browser, so a user can only ever see and change their own profile.
 */
@Service
public class ProfileService {

    private final AuthService authService;
    private final UserRepository userRepository;

    public ProfileService(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /** The profile of the signed-in user. 400 if sign-in is not finished yet (no account until the name is saved). */
    public ProfileResponse getProfile(String verifiedEmail) {
        User user = authService.requireAccount(verifiedEmail);
        return ProfileResponse.from(user, authService.assignRole(verifiedEmail));
    }

    /** Saves a new first and last name (already checked and cleaned up by UpdateProfileRequest). */
    @Transactional
    public ProfileResponse updateName(String verifiedEmail, UpdateProfileRequest request) {
        User user = authService.requireAccount(verifiedEmail);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return ProfileResponse.from(userRepository.save(user), authService.assignRole(verifiedEmail));
    }
}
