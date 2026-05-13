package fr.mossaab.security.unit.service;

import fr.mossaab.security.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests для сервиса JwtService")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private HttpServletRequest httpServletRequest;

    private static final String JWT_COOKIE_NAME = "jwt-cookie";
    private static final String SECRET_KEY = "7925633834416E396D7436753879382F423F4428482B4C6250655367566B5970";

    private Key signingKey;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 3600000L * 24);
        ReflectionTestUtils.setField(jwtService, "jwtCookieName", JWT_COOKIE_NAME);

        signingKey = jwtService.getSigningKey();
    }

    @Test
    @DisplayName("Извлечение имени пользователя из JWT токена - успешное")
    void extractUserName_ValidToken_Success() {
        String token = generateTestToken();

        String extractedUsername = jwtService.extractUserName(token);

        assertEquals("testUser", extractedUsername);
    }

    @Nested
    @DisplayName("Проверка валидности токена")
    class TokenValidation {

        @Test
        @DisplayName("Токен действителен - успешное")
        void isTokenValid_ValidToken_Success() {
            UserDetails userDetails1 = User.builder().username("testUser").roles("USER").password("password12345").build();
            String token = generateTestToken();

            assertTrue(jwtService.isTokenValid(token, userDetails1));
        }

        @Test
        @DisplayName("Токен истек - сбой")
        void isTokenExpired_ExpiredToken_Failure() {
            UserDetails expiredUserDetails = User.builder().username("testUser").roles("USER").password("password12345").build();
            String expiredToken = generateExpiredToken();

            Exception exception = assertThrows(RuntimeException.class, () -> {
                jwtService.isTokenValid(expiredToken, expiredUserDetails);
            });

            assertEquals("Invalid or expired token", exception.getMessage(), "Токен должен быть просрочен и вызвать исключение.");
        }
    }

    @Test
    @DisplayName("Генерация JWT токена - успешная")
    void generateToken_Success() {
        UserDetails userDetails1 = User.builder().username("testUser").roles("USER").password("password12345").build();

        String token = jwtService.generateToken(userDetails1);

        assertNotNull(token);
    }

    @Nested
    @DisplayName("Извлечение JWT из Cookies")
    class JwtFromCookies {

        @Test
        @DisplayName("Есть валидный cookie - успешное")
        void getJwtFromCookies_ExistingCookie_Success() {
            String token = generateTestToken();
            Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);

            try (MockedStatic<WebUtils> mockedWebUtils = mockStatic(WebUtils.class)) {
                mockedWebUtils.when(() -> WebUtils.getCookie(httpServletRequest, JWT_COOKIE_NAME)).thenReturn(cookie);

                String jwt = jwtService.getJwtFromCookies(httpServletRequest);

                assertEquals(token, jwt);
            }
        }

        @Test
        @DisplayName("Нет куки - пустое значение")
        void getJwtFromCookies_NoCookie_Null() {
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{});
            when(WebUtils.getCookie(httpServletRequest, JWT_COOKIE_NAME)).thenReturn(null);

            assertNull(jwtService.getJwtFromCookies(httpServletRequest));
        }

        @Test
        @DisplayName("Неверное имя cookie - пустое значение")
        void getJwtFromCookies_WrongCookieName_Null() {
            Cookie wrongCookie = new Cookie("anotherCookie", "testJwt");
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{wrongCookie});

            assertNull(jwtService.getJwtFromCookies(httpServletRequest));
        }
    }

    @Test
    @DisplayName("Генерация JWT Cookie - успешная")
    void generateJwtCookie_Success() {
        ResponseCookie cookie = jwtService.generateJwtCookie("jwtToken");

        assertEquals(JWT_COOKIE_NAME, cookie.getName());
        assertEquals("jwtToken", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    @DisplayName("Генерация чистого JWT Cookie - успешная")
    void getCleanJwtCookie_Success() {
        ResponseCookie cookie = jwtService.getCleanJwtCookie();

        assertEquals(JWT_COOKIE_NAME, cookie.getName());
        assertEquals("", cookie.getValue());
    }

    // Вспомогательные методы

    private String generateTestToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_USER");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("testUser")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_USER");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("testUser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000L)) // токен истек 2 часа назад
                .setExpiration(new Date(System.currentTimeMillis() - 3600000L)) // истек 1 час назад
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}