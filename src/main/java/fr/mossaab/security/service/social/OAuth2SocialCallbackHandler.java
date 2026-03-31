package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
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

@Component
@RequiredArgsConstructor
public class OAuth2SocialCallbackHandler implements AuthenticationSuccessHandler {

    private final OAuthUserInfoService socialAuthService;
    private final OneTimeAuthCodeService oneTimeAuthCodeService;
    private final SocialUserFlowService socialUserFlowService;

    @Value("${frontend.server.address}")
    private String frontendUrl;

    /**
     * Обработчик успешного завершения OAuth2-авторизации (login/registration/linking) через социального провайдера.
     * <p>
     * Анализирует наличие пользователя, email, socialId и перенаправляет на фронт с соответствующим кодом состояния (auth_status).
     * <p>
     * Статусы:
     * - SOCIAL_FOUND — socialId уже был, это логин существующего пользователя
     * - NEW_SOCIAL_USER — найден по email, socialId не было → надо привязать
     * - EMAIL_LINKED — этот email соц.сети уже занят другим аккаунтом (нельзя auto-link)
     * - NEW_ACCOUNT — полностью новый пользователь (нигде не был)
     * - ERROR — критическая ошибка
     * <p>
     * На фронте SPA обязан обработать параметры из query и запросить дальнейшие действия пользователя.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            sendRedirect(response, null, SocialAuthStatus.ERROR, "Ошибка аутентификации через соцсеть");
            return;
        }

        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
        OAuthProvider oAuthProvider = OAuthProvider.fromString(registrationId);
        if (oAuthProvider == null) {
            sendRedirect(response, null, SocialAuthStatus.ERROR, "Ошибка аутентификации, неизвестный провайдер");
            return;
        }

        OAuth2User oAuth2User = oauth2Token.getPrincipal();
        SocialUserInfo userInfo = socialAuthService.getUserInfo(oAuth2User, null, oAuthProvider);
        SocialUserFlowService.SocialAuthResult result = socialUserFlowService.analyzeUser(userInfo, oAuthProvider);

        // для SuccessHandler/redirect:
        response.sendRedirect(
                frontendUrl + "/?auth_status=" + result.getStatus().name().toLowerCase() +
                        "&auth_code=" + result.getAuthCode() +
                        "&message=" + URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8) +
                        "&provider=" + oAuthProvider
        );
    }


    private void sendRedirect(HttpServletResponse response, SocialUserInfo userInfo, SocialAuthStatus status, String message) throws IOException {
        StringBuilder url = new StringBuilder(frontendUrl + "/?auth_status=" + status.name().toLowerCase());
        if (status != SocialAuthStatus.ERROR) {
            String authCode = oneTimeAuthCodeService.issueCode(userInfo);
            url.append("&auth_code=").append(authCode);
        }
        if (message != null) {
            url.append("&message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));
        }
        response.sendRedirect(url.toString());
    }
}
