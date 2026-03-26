package fr.mossaab.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.entities.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenResponseService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public void sendTokenResponse(User user, HttpServletResponse response) {

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

        // Создаем куки
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(jwtToken);
        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(refreshToken);

        // Устанавливаем куки в ответ
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // Формируем JSON ответ
        Map<String, Object> responseBody = Map.of(
                "accessToken", jwtToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "email", user.getEmail()
        );

        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            new ObjectMapper().writeValue(response.getWriter(), responseBody);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при отправке ответа", e);
        }
    }
}

