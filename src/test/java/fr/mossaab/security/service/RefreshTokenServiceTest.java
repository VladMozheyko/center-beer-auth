package fr.mossaab.security.service;


import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.exception.TokenException;
import fr.mossaab.security.repository.RefreshTokenRepository;

import fr.mossaab.security.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 1000000L);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenName", "refreshToken");
    }

    @Test
    @DisplayName("Создание нового Refresh токена")
    void createRefreshToken_Success() {
        Long userId = 1L;
        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userId);

        assertNotNull(refreshToken);
        assertEquals(user, refreshToken.getUser());
        assertFalse(refreshToken.isRevoked());
        assertEquals(36, new String(Base64.getDecoder().decode(refreshToken.getToken())).length());
        assertTrue(refreshToken.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Проверка истечения срока действия Refresh токена")
    void verifyExpiration_ExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expiredToken")
                .expiryDate(Instant.now().minusSeconds(1))
                .build();

        assertThrows(TokenException.class, () -> refreshTokenService.verifyExpiration(expiredToken));

        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }

    @Test
    @DisplayName("Успешное нахождение Refresh токена")
    void findByToken_Success() {
        String tokenValue = "test-token";
        RefreshToken refreshToken = RefreshToken.builder().token(tokenValue).build();

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> foundToken = refreshTokenService.findByToken(tokenValue);

        assertTrue(foundToken.isPresent());
        assertEquals(tokenValue, foundToken.get().getToken());
    }

    @Test
    @DisplayName("Извлечение токена из cookie")
    void getRefreshTokenFromCookies_Success() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie expectedCookie = new Cookie("refreshToken", "testRefreshToken");

        try (MockedStatic<WebUtils> webUtilsMockedStatic = mockStatic(WebUtils.class);) {
            webUtilsMockedStatic.when(() -> WebUtils.getCookie(request, "refreshToken"))
                    .thenReturn(expectedCookie);

            String token = refreshTokenService.getRefreshTokenFromCookies(request);

            assertEquals("testRefreshToken", token);
        }
    }

    @Test
    @DisplayName("Удаление Refresh токена по значению")
    void deleteByToken_Success() {
        String tokenValue = "test-token";
        RefreshToken refreshToken = RefreshToken.builder().token(tokenValue).build();

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.deleteByToken(tokenValue);

        verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }

    @Test
    @DisplayName("Создание пустого cookie для удаления токена")
    void getCleanRefreshTokenCookie_Success() {
        ResponseCookie cookie = refreshTokenService.getCleanRefreshTokenCookie();

        assertEquals("refreshToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals(0, cookie.getMaxAge().getSeconds());
    }
}