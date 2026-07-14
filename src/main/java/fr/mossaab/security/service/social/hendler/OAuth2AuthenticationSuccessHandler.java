package fr.mossaab.security.service.social.hendler;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.service.social.service.OAuthUserInfoService;
import fr.mossaab.security.service.social.service.SocialUserFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик успешной OAuth2-аутентификации.
 * Перенаправляет на фронт с параметрами: статус, код, сообщение.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthUserInfoService userInfoService;
    private final SocialUserFlowService flowService;
    private final OAuthStateStorage stateStorage;

    @Value("${frontend.server.address}")
    private String frontendUrl;
    @Value("${mobile.server.address}")
    private String mobileUrl;

    @Value("${web.server.address}")
    private String webUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        log.info("[OAuth2 Handler] - Процесс обработки ответа аутентификации через соцсеть");
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            log.error("[OAuth2 Handler] - Ошибка типа аутентификации");
//            sendErrorRedirect(response, SocialAuthStatus.ERROR, "Тип недействительной аутентификации");
            sendErrorRedirect(response, request, SocialAuthStatus.ERROR, "Тип недействительной аутентификации");
            return;
        }

        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuthProvider provider = OAuthProvider.fromString(registrationId);
        if (provider == null) {
            log.error("[OAuth2 Handler] - Не удалось определить провайдера аутентификации или неподдерживаемый тип");
//            sendErrorRedirect(response, SocialAuthStatus.ERROR, "Неизвестный провайдер OAuth");
            sendErrorRedirect(response, request, SocialAuthStatus.ERROR, "Неизвестный провайдер OAuth");
            return;
        }

        OAuth2User oAuth2User = token.getPrincipal();
        SocialUserInfo userInfo = userInfoService.getUserInfo(oAuth2User, null, provider);
        SocialUserFlowService.SocialAuthResult result = flowService.analyzeUser(userInfo, provider);

        // Проверка state параметра для защиты от CSRF (опционально, для обратной совместимости)
        String springState = request.getParameter("state");
        if (springState != null) {
            String storedState = stateStorage.get(springState);
            if (storedState == null) {
                log.warn("[OAuth2 Handler] - Недействительный state параметр, возможна CSRF атака");
                sendErrorRedirect(response, SocialAuthStatus.ERROR, "Недействительный state параметр");
                return;
            }
            // Удаляем использованный state
            stateStorage.remove(springState);
        }

        String redirectUrl = frontendUrl + "?" +
                "auth_status=" + result.getStatus().name().toLowerCase() +
                "&auth_code=" + result.getAuthCode() +
                "&message=" + encode(result.getMessage()) +
                "&provider=" + provider;
        response.sendRedirect(redirectUrl);
        log.info("[OAuth2 Handler] -  Успешная авторизация через {}", provider);
    }

    private void sendErrorRedirect(HttpServletResponse response, HttpServletRequest request, SocialAuthStatus status, String message) throws IOException {
        String url = getRedirectUrl(request) + "?auth_status=" + status.name().toLowerCase() +
                "&message=" + encode(message);
        response.sendRedirect(url);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}