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
            sendErrorRedirect(response, request, SocialAuthStatus.ERROR, "Тип недействительной аутентификации");
            return;
        }

        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuthProvider provider = OAuthProvider.fromString(registrationId);
        if (provider == null) {
            log.error("[OAuth2 Handler] - Не удалось определить провайдера аутентификации или неподдерживаемый тип");
            sendErrorRedirect(response, request, SocialAuthStatus.ERROR, "Неизвестный провайдер OAuth");
            return;
        }

        OAuth2User oAuth2User = token.getPrincipal();
        SocialUserInfo userInfo = userInfoService.getUserInfo(oAuth2User, null, provider);
        SocialUserFlowService.SocialAuthResult result = flowService.analyzeUser(userInfo, provider);

        String redirectUrl = getRedirectUrl(request) + "?" +
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

    private String getRedirectUrl(HttpServletRequest request) {
//        String state = request.getParameter("state");
//        boolean isMobile = state != null && state.startsWith("mobile_");
//        log.info("[STATE_DEVICE] state={}", state);

//        String redirectUrlBase;
//        if (isMobile) {
//            redirectUrlBase = "centerbeer://oauth-callback" /* мобильный deep link */;
//            redirectUrlBase = mobileUrl;
//        } else {
//            redirectUrlBase = webUrl;
//            redirectUrlBase = "https://center.beer/app" /* обычный веб */;
//        }
//        return redirectUrlBase;
	  return mobileUrl;
    }
}
