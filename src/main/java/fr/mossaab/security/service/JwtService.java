package fr.mossaab.security.service;

import fr.mossaab.security.controller.AdminController;
import fr.mossaab.security.exception.TokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Реализация интерфейса JwtService, предоставляющая методы для создания и обработки JWT токенов.
 */
@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    @Value("${application.security.jwt.cookie-name}")
    private String jwtCookieName;

    /**
     * Извлекает имя пользователя из JWT токена.
     *
     * @param token JWT токен
     * @return Имя пользователя, связанное с токеном
     */
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Генерирует JWT токен на основе информации о пользователе.
     *
     * @param userDetails Информация о пользователе
     * @return Сгенерированный JWT токен
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Проверяет, действителен ли JWT токен для указанного пользователя.
     *
     * @param token       JWT токен
     * @param userDetails Информация о пользователе
     * @return true, если токен действителен для пользователя, иначе false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Проверяет, истек ли срок действия JWT токена.
     *
     * @param token JWT токен
     * @return true, если срок действия токена истек, иначе false
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Извлекает дату истечения срока действия JWT токена.
     *
     * @param token JWT токен
     * @return Дата истечения срока действия токена
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Генерирует JWT токен на основе информации о пользователе и дополнительных параметров.
     *
     * @param extraClaims  Дополнительные данные для включения в токен
     * @param userDetails Информация о пользователе
     * @return Сгенерированный JWT токен
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Генерирует HTTP Cookie с JWT токеном.
     *
     * @param jwt JWT токен
     * @return HTTP Cookie с JWT токеном
     */
    public ResponseCookie generateJwtCookie(String jwt) {
        return ResponseCookie.from(jwtCookieName, jwt)
                .path("/")
                .maxAge(24 * 60 * 60 * 300) // 24 часа
//                .httpOnly(false)
//                .secure(false)
//                .sameSite("Strict")
                .httpOnly(true)
                .secure(true) // Убедитесь, что флаг secure установлен для HTTPS
                .sameSite("None") // Для кросс-доменных запросов
                .build();
    }

    /**
     * Извлекает JWT токен из HTTP Cookies.
     *
     * @param request HTTP запрос
     * @return JWT токен, извлеченный из Cookies
     */
    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookieName);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    /**
     * Возвращает пустой HTTP Cookie для удаления JWT токена.
     *
     * @return Пустой HTTP Cookie
     */
    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookieName, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();
    }

    /**
     * Строит JWT токен на основе предоставленных данных.
     *
     * @param extraClaims  Дополнительные данные для включения в токен
     * @param userDetails Информация о пользователе
     * @param expiration   Срок действия токена в миллисекундах
     * @return Построенный JWT токен
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Role not found"))
                .getAuthority();
        extraClaims.put("role", role);

        logger.debug("Building token with claims: {}", extraClaims);
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Извлекает указанное свойство из JWT токена.
     *
     * @param token           JWT токен
     * @param claimsResolvers Функция для извлечения свойства из Claims
     * @param <T>             Тип извлекаемого свойства
     * @return Извлеченное свойство из токена
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    /**
     * Извлекает все данные (Claims) из JWT токена.
     *
     * @param token JWT токен
     * @return Объект Claims, содержащий все данные из токена
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            logger.warn("JWT processing failed: {}", e.getMessage()); // или log.error, если критично
            throw new RuntimeException("Invalid or expired token");
        }
    }


    public String extractRole(String token) {
        logger.debug("Extracted role: {}");
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    /**
     * Получает ключ для подписи JWT токена на основе секретного ключа.
     *
     * @return Ключ для подписи JWT токена
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}