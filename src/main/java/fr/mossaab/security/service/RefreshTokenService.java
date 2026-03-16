package fr.mossaab.security.service;

import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.exception.TokenException;
import fr.mossaab.security.repository.RefreshTokenRepository;
import fr.mossaab.security.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация интерфейса RefreshTokenService, обеспечивающая создание, проверку и обновление Refresh токенов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    @Value("${application.security.jwt.refresh-token.cookie-name}")
    private String refreshTokenName;

    /**
     * Создает Refresh токен для указанного пользователя.
     *
     * @param userId ID пользователя
     * @return Созданный Refresh токен
     */
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        RefreshToken refreshToken = RefreshToken.builder()
                .revoked(false)
                .user(user)
                .token(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()))
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Проверяет, не истек ли срок действия Refresh токена.
     *
     * @param token Refresh токен для проверки
     * @return Refresh токен
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if(token == null){
            log.error("Token is null");
            throw new TokenException(null, "Token is null");
        }
        if(token.getExpiryDate().compareTo(Instant.now()) < 0 ){
            refreshTokenRepository.delete(token);
            throw new TokenException(token.getToken(), "Refresh token was expired. Please make a new authentication request");
        }
        return token;
    }

    /**
     * Находит Refresh токен по его значению.
     *
     * @param token Значение Refresh токена
     * @return Опциональный объект Refresh токена
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Генерирует новый JWT токен на основе Refresh токена.
     *
     * @param request Запрос на обновление токена
     * @return Ответ с новым JWT токеном и Refresh токеном
     */
    public AuthenticationService.RefreshTokenResponse generateNewToken(AuthenticationService.RefreshTokenRequest request) {
        User user = refreshTokenRepository.findByToken(request.getRefreshToken())
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .orElseThrow(() -> new TokenException(request.getRefreshToken(),"Refresh token does not exist"));

        String token = jwtService.generateToken(user);
        return AuthenticationService.RefreshTokenResponse.builder()
                .accessToken(token)
                .refreshToken(request.getRefreshToken())
                .tokenType(TokenType.BEARER.name())
                .build();
    }

    /**
     * Генерирует HTTP Cookie для Refresh токена.
     *
     * @param token Значение Refresh токена
     * @return HTTP Cookie с Refresh токеном
     */
    public ResponseCookie generateRefreshTokenCookie(String token) {
        return ResponseCookie.from(refreshTokenName, token)
                .path("/")
                .maxAge(refreshExpiration/1000 * 20) // 15 дней в секундах
//                .httpOnly(false)
//                .secure(false)
//                .sameSite("Strict")
                .httpOnly(true) // Убедитесь, что куки доступны только на сервере
                .secure(true) // Для HTTPS
                .sameSite("None") // Для кросс-доменных запросов
                .build();
    }

    /**
     * Извлекает Refresh токен из HTTP Cookies.
     *
     * @param request HTTP запрос
     * @return Значение Refresh токена из Cookies
     */
    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, refreshTokenName);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return "";
        }
    }

    /**
     * Удаляет Refresh токен из базы данных по его значению.
     *
     * @param token Значение Refresh токена
     */
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Создает пустой HTTP Cookie для удаления Refresh токена.
     *
     * @return Пустой HTTP Cookie
     */
    public ResponseCookie getCleanRefreshTokenCookie() {
        return ResponseCookie.from(refreshTokenName, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();
    }
}
