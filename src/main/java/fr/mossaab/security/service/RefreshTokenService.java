package fr.mossaab.security.service;

import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.exception.TokenException;
import fr.mossaab.security.repository.RefreshTokenRepository;
import fr.mossaab.security.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Реализация интерфейса RefreshTokenService, обеспечивающая создание, проверку и обновление Refresh токенов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    @Value("${application.security.jwt.refresh-token.cookie-name}")
    private String refreshTokenName;

    // Максимум одновременных устройств
    private static final int MAX_SESSIONS_PER_USER = 3;

    /**
     * Создать новый refresh-токен или переиспользовать существующий для этого устройства.
     * Если deviceId == null/пустой – будет сгенерирован новый.
     */
    @Transactional
    public RefreshToken createOrReuseRefreshToken(Long userId, String deviceId, String deviceInfo) {
        Instant now = Instant.now();

        //Пытаемся реюзить только если deviceId прислан
        if (deviceId != null && !deviceId.isBlank()) {

            Optional<RefreshToken> existingOpt =
                    refreshTokenRepository.findByUserIdAndDeviceIdAndExpiryDateAfter(userId, deviceId, now);

            if (existingOpt.isPresent()) {
                RefreshToken existing = existingOpt.get();

                // проверяем истечение
                existing = verifyExpiration(existing); // допустим, возвращает null, если истек и удален/отозван
                if (existing != null && !existing.isRevoked()) {
                    existing.setLastUsedAt(Instant.now());
                    if (deviceInfo != null) {
                        existing.setDeviceInfo(deviceInfo);
                    }
                    return refreshTokenRepository.save(existing);
                }
                // если истек / отозван → падаем ниже и будем создавать НОВУЮ цепочку с НОВЫМ deviceId
            }
            // если existingOpt пустой или existing невалиден, НЕЛЬЗЯ дальше использовать
            // старый deviceId. Считаем его мертвым и ОБНУЛЯЕМ:
            deviceId = null;
        }

        // Если deviceId отсутствует или был признан мертвым — создаем НОВЫЙ
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }

        // Проверка лимита сессий
        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUserIdAndExpiryDateAfter(userId, now)
                        .stream()
                        .filter(rt -> !rt.isRevoked())
                        .toList();

        if (activeTokens.size() >= MAX_SESSIONS_PER_USER) {
            activeTokens.stream()
                    .min(Comparator.comparing(rt ->
                            rt.getCreatedAt() != null ? rt.getCreatedAt() : Instant.now())
                    ).ifPresent(oldest -> {
                        oldest.setRevoked(true);
                        refreshTokenRepository.save(oldest);
                    });
        }

        // Создаем НОВЫЙ refresh-токен (deviceId уже либо клиентский валидный, либо новый)
        String token = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
        Instant expiryDate = now.plusMillis(refreshExpiration);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(User.builder().id(userId).build())
                .expiryDate(expiryDate)
                .revoked(false)
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .deviceId(deviceId)
                .deviceInfo(deviceInfo)
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
    @Transactional
    public AuthenticationService.RefreshTokenResponse generateNewToken(
            AuthenticationService.RefreshTokenRequest request
    ) {
        String requestToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired or revoked");
        }

        // обновляем время последнего использования
        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
        String deviceId = refreshTokenRepository.findDeviceIdByToken(requestToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user, deviceId);

        return AuthenticationService.RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
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
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
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

    @Transactional
    public void deleteAllByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> getAllByUserId(Long userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteEverythingExceptTheCurrentDevice(String refreshToken, Long userId) {
        refreshTokenRepository.deleteByUserIdWhereNotThisToken(userId, refreshToken);
    }
}
