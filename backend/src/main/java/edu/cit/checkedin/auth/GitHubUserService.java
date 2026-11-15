package edu.cit.checkedin.auth;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import edu.cit.checkedin.integration.IdentityProvider;
import edu.cit.checkedin.integration.IdentityProviderException;

/**
 * Decides whether a GitHub account may sign in. Spring Security calls this during "Sign in with
 * GitHub", right after it has swapped GitHub's one-time code for an access token on the server
 * (NFR-008). Covers FR-002: the account is accepted only if GitHub has VERIFIED a @cit.edu address
 * on it. If not, this throws, so Spring never treats the person as signed in.
 */
@Service
public class GitHubUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    // Names of the two values we hand on to GitHubLoginHandler
    public static final String EMAIL_ATTRIBUTE = "email";
    public static final String GITHUB_ID_ATTRIBUTE = "githubId";
    // Error codes sent back to the sign-in page, which turns them into plain-language messages
    public static final String NOT_CIT_EMAIL = "not-cit-email";
    public static final String GITHUB_FAILED = "github-failed";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> githubProfileLoader;
    private final IdentityProvider identityProvider;
    private final EmailAddressChecker emailAddressChecker;

    /** Used by Spring: reads the GitHub profile with Spring Security's standard loader. */
    @Autowired
    public GitHubUserService(IdentityProvider identityProvider, EmailAddressChecker emailAddressChecker) {
        this(new DefaultOAuth2UserService(), identityProvider, emailAddressChecker);
    }

    /** Used by the tests, which pass a fake profile loader instead of calling GitHub. */
    GitHubUserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> githubProfileLoader,
                      IdentityProvider identityProvider, EmailAddressChecker emailAddressChecker) {
        this.githubProfileLoader = githubProfileLoader;
        this.identityProvider = identityProvider;
        this.emailAddressChecker = emailAddressChecker;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // GET https://api.github.com/user: we only need the account's numeric ID from it.
        // (We never use the "email" in this profile: it is public, optional, and may be unverified.)
        OAuth2User githubProfile = githubProfileLoader.loadUser(userRequest);
        Number githubId = githubProfile.getAttribute("id");

        String citEmail = findVerifiedCitEmail(userRequest.getAccessToken().getTokenValue());

        // The verified @cit.edu email becomes the signed-in name, never the GitHub ID
        Map<String, Object> attributes = Map.of(EMAIL_ATTRIBUTE, citEmail, GITHUB_ID_ATTRIBUTE, githubId.longValue());
        return new DefaultOAuth2User(List.of(), attributes, EMAIL_ATTRIBUTE);
    }

    private String findVerifiedCitEmail(String accessToken) {
        List<String> verifiedEmails;
        try {
            verifiedEmails = identityProvider.getVerifiedEmails(accessToken);
        } catch (IdentityProviderException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error(GITHUB_FAILED),
                    "Could not read the email addresses on the GitHub account.", exception);
        }
        return emailAddressChecker.findFirstAllowed(verifiedEmails)
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error(NOT_CIT_EMAIL),
                        "The GitHub account has no verified @cit.edu email address."));
    }
}
