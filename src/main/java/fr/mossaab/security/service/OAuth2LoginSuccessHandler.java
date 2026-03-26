package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenResponseService tokenResponseService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String email = null;
        String name = null;

        if ("google".equals(provider)) {
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
        } else if ("yandex".equals(provider)) {
            email = oauth2User.getAttribute("default_email");
            if (email == null) email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("real_name");
            if (name == null) name = oauth2User.getAttribute("first_name");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = User.builder()
                    .email(email)
                    .nickname(name.replaceAll("\\s+", "_") + "_" + UUID.randomUUID().toString().substring(0, 5))
                    .role(Role.USER)
                    .temporarySecondsBalance(0)
                    .phoneVerified(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
        }

        tokenResponseService.sendTokenResponse(user, response); //заменил нижеуказанный код, работает так же

//        String jwtToken = jwtService.generateToken(user);
//        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
//
//        // Устанавливаем куки (если нужно)
//        ResponseCookie jwtCookie = jwtService.generateJwtCookie(jwtToken);
//        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(refreshToken);
//        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
//        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
//
//        // Устанавливаем тип ответа и отправляем JSON
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//
//        String jsonResponse = String.format("""
//        {
//            "accessToken": "%s",
//            "refreshToken": "%s",
//            "tokenType": "Bearer",
//            "email": "%s"
//        }
//        """, jwtToken, refreshToken, email);
//
//        response.getWriter().write(jsonResponse);
    }


}
