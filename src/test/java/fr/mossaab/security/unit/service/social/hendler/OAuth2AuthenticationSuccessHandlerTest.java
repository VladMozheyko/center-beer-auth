package fr.mossaab.security.unit.service.social.hendler;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.service.social.hendler.OAuth2AuthenticationSuccessHandler;
import fr.mossaab.security.service.social.service.OAuthUserInfoService;
import fr.mossaab.security.service.social.service.SocialUserFlowService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-test для OAuth2AuthenticationSuccessHandler")
class OAuth2AuthenticationSuccessHandlerTest {

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    @Mock
    private OAuthUserInfoService userInfoService;

    @Mock
    private SocialUserFlowService flowService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuth2AuthenticationToken authentication;

    @Mock
    private OAuth2User oAuth2User;

    private SocialUserFlowService.SocialAuthResult authResult;
    private final String frontendUrl = "http://localhost";

    @BeforeEach
    void setUp() {
        authResult = new SocialUserFlowService.SocialAuthResult(
                SocialAuthStatus.NEW_ACCOUNT,
                "New account can be created",
                "test@example.com",
                null,
                "auth-code"
        );
    }

    @Test
    @DisplayName("Redirects to frontend on successful authentication")
    void onAuthenticationSuccess_shouldRedirectOnSuccess() throws IOException {
        ReflectionTestUtils.setField(successHandler, "frontendUrl", frontendUrl);
        OAuth2User oAuth2User = mock(OAuth2User.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "12345");
        attributes.put("email", "user@example.com");

        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(userInfoService.getUserInfo(
                eq(oAuth2User),
                eq(null),
                eq(OAuthProvider.GOOGLE)
        )).thenReturn(new SocialUserInfo());

        when(flowService.analyzeUser(any(SocialUserInfo.class), any(OAuthProvider.class)))
                .thenReturn(authResult);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(startsWith("http://localhost?auth_status=new_account"));
    }

    @Test
    @DisplayName("Handles invalid OAuth2 authentication types gracefully")
    void onAuthenticationSuccess_shouldHandleInvalidAuthentication() throws IOException {
        Authentication invalidAuthentication = mock(Authentication.class);
        successHandler.onAuthenticationSuccess(request, response, invalidAuthentication);

        verify(response).sendRedirect(contains("auth_status=error"));
    }

    @Test
    @DisplayName("Throws exception on unknown OAuth provider")
    void onAuthenticationSuccess_shouldThrowOnUnknownProvider() throws IOException {
        ReflectionTestUtils.setField(successHandler, "frontendUrl", frontendUrl);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("unknown");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectUrlCaptor.capture());

        String actualUrl = redirectUrlCaptor.getValue();
        String decodedUrl = URLDecoder.decode(actualUrl, StandardCharsets.UTF_8);

        Assertions.assertTrue(decodedUrl.contains("auth_status=error"));
        Assertions.assertTrue(decodedUrl.contains("message=Неизвестный провайдер OAuth"));
    }

    @Test
    @DisplayName("Encodes URL parameters correctly")
    void onAuthenticationSuccess_shouldEncodeUrlParameters() throws IOException {
        String specialMessage = "Message with spaces & special=characters!";
        authResult.setMessage(specialMessage);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(userInfoService.getUserInfo(any(OAuth2User.class), any(), any())).thenReturn(new SocialUserInfo());
        when(flowService.analyzeUser(any(), any())).thenReturn(authResult);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains(URLEncoder.encode(specialMessage, StandardCharsets.UTF_8)));
    }
}