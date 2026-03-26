package fr.mossaab.security.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.service.TokenResponseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialAuthenticationService socialAuthService;
    private final TokenResponseService tokenResponseService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            sendJsonError(response, HttpStatus.UNAUTHORIZED.value(), "access_denied", "User is not authenticated");
            return;
        }

        String registrationId = oauth2Token.getAuthorizedClientRegistrationId(); // "yandex", "google"
        OAuth2User oAuth2User = oauth2Token.getPrincipal();

        try {
            User user = socialAuthService.authenticateOrRegister(registrationId, oAuth2User);
            tokenResponseService.sendTokenResponse(user, response);
        } catch (Exception e) {
            sendJsonError(response, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "auth_failed", "User is not authenticated");
        }
    }

    /**
     * Унифицированный JSON-ответ об ошибке
     */
    private void sendJsonError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", error,
                "message", message
        ));
    }
}
