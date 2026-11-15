package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import edu.cit.checkedin.integration.IdentityProvider;
import edu.cit.checkedin.integration.IdentityProviderException;

/**
 * Tests the FR-002 rule for GitHub: only accounts with a verified @cit.edu email may sign in.
 * GitHub itself is faked: the profile loader and the email list are both stand-ins.
 */
@ExtendWith(MockitoExtension.class)
class GitHubUserServiceTest {

    @Mock
    private IdentityProvider identityProvider;

    private GitHubUserService service;
    private OAuth2UserRequest userRequest;

    @BeforeEach
    void setUp() {
        // Stand-in for "GET https://api.github.com/user": this GitHub account's ID is 583231
        OAuth2UserService<OAuth2UserRequest, OAuth2User> fakeProfileLoader = request ->
                new DefaultOAuth2User(List.of(), Map.of("id", 583231, "login", "juandc"), "id");
        service = new GitHubUserService(fakeProfileLoader, identityProvider,
                new EmailAddressChecker(List.of("cit.edu")));

        ClientRegistration github = CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId("test-client-id").clientSecret("test-client-secret").build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-token", Instant.now(), Instant.now().plusSeconds(60));
        userRequest = new OAuth2UserRequest(github, accessToken);
    }

    @Test
    void acceptsAnAccountWithAVerifiedCitEduEmail() {
        when(identityProvider.getVerifiedEmails("test-token"))
                .thenReturn(List.of("juan.personal@gmail.com", "Juan.DelaCruz@CIT.edu"));

        OAuth2User signedInUser = service.loadUser(userRequest);

        // The signed-in name is the verified @cit.edu email, never the GitHub ID
        assertThat(signedInUser.getName()).isEqualTo("juan.delacruz@cit.edu");
        assertThat((Long) signedInUser.getAttribute(GitHubUserService.GITHUB_ID_ATTRIBUTE)).isEqualTo(583231L);
    }

    @Test
    void rejectsAnAccountWhoseVerifiedEmailsAreNotCitEdu() {
        // GitHubIdentityProvider already removed unverified addresses (see GitHubIdentityProviderTest)
        when(identityProvider.getVerifiedEmails("test-token")).thenReturn(List.of("juan@gmail.com"));

        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
                        assertThat(exception.getError().getErrorCode()).isEqualTo(GitHubUserService.NOT_CIT_EMAIL));
    }

    @Test
    void failsSafelyWhenTheEmailListCannotBeRead() {
        when(identityProvider.getVerifiedEmails("test-token"))
                .thenThrow(new IdentityProviderException("GitHub is down", new RuntimeException()));

        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
                        assertThat(exception.getError().getErrorCode()).isEqualTo(GitHubUserService.GITHUB_FAILED));
    }
}
