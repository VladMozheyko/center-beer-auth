package fr.mossaab.security.service.social.hendler;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.service.social.service.OAuthUserInfoService;
import fr.mossaab.security.service.social.service.SocialUserFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик успешной OAuth2-аутентификации.
 * Перенаправляет на фронт с параметрами: статус, код, сообщение.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthUserInfoService userInfoService;
    private final SocialUserFlowService flowService;

    @Value("${frontend.server.address}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            sendErrorRedirect(response, SocialAuthStatus.ERROR, "Invalid authentication type");
            return;
        }

        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuthProvider provider = OAuthProvider.fromString(registrationId);
        if (provider == null) {
            sendErrorRedirect(response, SocialAuthStatus.ERROR, "Unknown OAuth provider");
            return;
        }

        OAuth2User oAuth2User = token.getPrincipal();
        SocialUserInfo userInfo = userInfoService.getUserInfo(oAuth2User, null, provider);
        SocialUserFlowService.SocialAuthResult result = flowService.analyzeUser(userInfo, provider);

        String redirectUrl = frontendUrl + "?" +
                "auth_status=" + result.getStatus().name().toLowerCase() +
                "&auth_code=" + result.getAuthCode() +
                "&message=" + encode(result.getMessage()) +
                "&provider=" + provider;

        response.sendRedirect(redirectUrl);
    }

    private void sendErrorRedirect(HttpServletResponse response, SocialAuthStatus status, String message) throws IOException {
        String url = frontendUrl + "?auth_status=" + status.name().toLowerCase() +
                "&message=" + encode(message);
        response.sendRedirect(url);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}