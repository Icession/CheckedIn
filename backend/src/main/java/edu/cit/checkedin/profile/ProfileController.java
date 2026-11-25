package edu.cit.checkedin.profile;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.profile.dto.ProfileResponse;
import edu.cit.checkedin.profile.dto.UpdateProfileRequest;

/**
 * The signed-in user's own profile, for professors and students (Increment 2 / SRS v1.4).
 *
 * GET /api/profile                         -> { email, firstName, lastName, role, studentNumber, createdOn }
 * PUT /api/profile { firstName, lastName } -> the updated profile; 400 with a message if a name breaks the rules
 *
 * There is no user ID in the URL or the body: both always use the person signed in on this session
 * (Authentication, filled by Spring Security from the session cookie), so nobody can see or change someone
 * else's profile. Signed out -> 401 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping
    public ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return profileService.updateName(authentication.getName(), request);
    }
}
