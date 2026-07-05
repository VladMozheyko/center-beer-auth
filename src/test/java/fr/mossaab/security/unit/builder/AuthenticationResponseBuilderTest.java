package fr.mossaab.security.unit.builder;

import fr.mossaab.security.builder.AuthenticationResponseBuilder;
import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.AuthenticationResponseDto;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.UserIpTempService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Unit-тесты сборщика ответов аутентификации")
class AuthenticationResponseBuilderTest {

    @Mock
    private UserIpTempService userIpTempService;

    @Mock
    private IpHelper ipHelper;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthenticationResponseBuilder authenticationResponseBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Проверка сборки ответа с куки и правильной установкой заголовков")
    void testBuildResponseWithCookies_Success() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        AuthenticationResponse response = AuthenticationResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(List.of("ROLE_USER"))
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .tokenType("BEARER")
                .deviceId("device-uuid-789")
                .userIPs(List.of(
                        UserIpTempDto.builder()
                                .ipAddress("192.168.1.1")
                                .createdAt(Instant.now())
                                .build()
                ))
                .jwtCookie(ResponseCookie.from("jwt", "jwt-token").build())
                .refreshTokenCookie(ResponseCookie.from("refresh", "refresh-token").build())
                .build();

        String message = "Успешный вход";

        // When
        ResponseEntity<AuthenticationResponseDto> result = authenticationResponseBuilder
                .buildResponseWithCookies(response, message);

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());

        AuthenticationResponseDto dto = result.getBody();
        assertNotNull(dto);
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("access-token-123", dto.getAccessToken());
        assertEquals("refresh-token-456", dto.getRefreshToken());
        assertEquals("device-uuid-789", dto.getDeviceId());
        assertEquals("Успешный вход", dto.getMessage());
        assertEquals("200", dto.getStatus());
        assertEquals(1, dto.getLastIpAddress().size());
        assertEquals("192.168.1.1", dto.getLastIpAddress().get(0).getIpAddress());

        List<String> cookies = result.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertEquals(2, cookies.size());
    }

    @Test
    @DisplayName("Проверка создания AuthenticationResponse с правильными токенами и IP")
    void testBuildAuthenticationResponse_Success() {
        // Given
        Long userId = 1L;
        String deviceId = "device-uuid-789";
        String jwtToken = "jwt-token-123";
        String refreshTokenValue = "refresh-token-456";
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(fr.mossaab.security.enums.Role.USER)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token(refreshTokenValue)
                .deviceId(deviceId)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        UserIpTempDto ipDto = UserIpTempDto.builder()
                .ipAddress(clientIp)
                .createdAt(Instant.now())
                .build();

        // Mocking
        when(ipHelper.getClientIp(request)).thenReturn(clientIp);
        doNothing().when(userIpTempService).saveIpTemp(userId, clientIp);
        when(userIpTempService.getTrackedIpForUser(userId)).thenReturn(List.of(ipDto));
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        when(refreshTokenService.createOrReuseRefreshToken(userId, deviceId, userAgent))
                .thenReturn(refreshToken);
        when(jwtService.generateToken(user, deviceId)).thenReturn(jwtToken);
        when(refreshTokenService.generateRefreshTokenCookie(jwtToken))
                .thenReturn(ResponseCookie.from("refresh", refreshTokenValue).build());
        when(jwtService.generateJwtCookie(jwtToken))
                .thenReturn(ResponseCookie.from("jwt", jwtToken).build());

        // When
        AuthenticationResponse result = authenticationResponseBuilder
                .buildAuthenticationResponse(user, deviceId, request);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(List.of("ROLE_USER"), result.getRoles());
        assertEquals(jwtToken, result.getAccessToken());
        assertEquals(refreshTokenValue, result.getRefreshToken());
        assertEquals("BEARER", result.getTokenType());
        assertEquals(deviceId, result.getDeviceId());
        assertEquals(1, result.getUserIPs().size());
        assertEquals(clientIp, result.getUserIPs().get(0).getIpAddress());

        assertNotNull(result.getJwtCookie());
        assertNotNull(result.getRefreshTokenCookie());

        verify(ipHelper, times(1)).getClientIp(request);
        verify(userIpTempService, times(1)).saveIpTemp(userId, clientIp);
        verify(userIpTempService, times(1)).getTrackedIpForUser(userId);
        verify(refreshTokenService, times(1)).createOrReuseRefreshToken(userId, deviceId, userAgent);
        verify(jwtService, times(1)).generateToken(user, deviceId);
        verify(jwtService, times(1)).generateJwtCookie(jwtToken);
        verify(refreshTokenService, times(1)).generateRefreshTokenCookie(jwtToken);
    }

    @Test
    @DisplayName("Проверка перегруженного метода сборки ответа с пользователем и запросом")
    void testBuildResponseWithCookies_WithUserAndRequest() {
        // Given
        Long userId = 1L;
        String deviceId = "device-uuid-789";
        String jwtToken = "jwt-token-123";
        String refreshTokenValue = "refresh-token-456";
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String message = "Успешная регистрация";

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(fr.mossaab.security.enums.Role.USER)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token(refreshTokenValue)
                .deviceId(deviceId)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        UserIpTempDto ipDto = UserIpTempDto.builder()
                .ipAddress(clientIp)
                .createdAt(Instant.now())
                .build();

        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .id(userId)
                .email("test@example.com")
                .roles(List.of("ROLE_USER"))
                .accessToken(jwtToken)
                .refreshToken(refreshTokenValue)
                .tokenType("BEARER")
                .deviceId(deviceId)
                .userIPs(List.of(ipDto))
                .jwtCookie(ResponseCookie.from("jwt", jwtToken).build())
                .refreshTokenCookie(ResponseCookie.from("refresh", refreshTokenValue).build())
                .build();

        // Mocking for buildAuthenticationResponse
        when(ipHelper.getClientIp(request)).thenReturn(clientIp);
        doNothing().when(userIpTempService).saveIpTemp(userId, clientIp);
        when(userIpTempService.getTrackedIpForUser(userId)).thenReturn(List.of(ipDto));
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        when(refreshTokenService.createOrReuseRefreshToken(userId, deviceId, userAgent))
                .thenReturn(refreshToken);
        when(jwtService.generateToken(user, deviceId)).thenReturn(jwtToken);
        when(refreshTokenService.generateRefreshTokenCookie(jwtToken))
                .thenReturn(ResponseCookie.from("refresh", refreshTokenValue).build());
        when(jwtService.generateJwtCookie(jwtToken))
                .thenReturn(ResponseCookie.from("jwt", jwtToken).build());

        // When
        ResponseEntity<AuthenticationResponseDto> result = authenticationResponseBuilder
                .buildResponseWithCookies(user, message, request, deviceId);

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());

        AuthenticationResponseDto dto = result.getBody();
        assertNotNull(dto);
        assertEquals("test@example.com", dto.getEmail());
        assertEquals(jwtToken, dto.getAccessToken());
        assertEquals(refreshTokenValue, dto.getRefreshToken());
        assertEquals(deviceId, dto.getDeviceId());
        assertEquals(message, dto.getMessage());
        assertEquals("200", dto.getStatus());
        assertEquals(1, dto.getLastIpAddress().size());

        List<String> cookies = result.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertEquals(2, cookies.size());

        // Verify all methods were called
        verify(ipHelper, times(1)).getClientIp(request);
        verify(userIpTempService, times(1)).saveIpTemp(userId, clientIp);
        verify(userIpTempService, times(1)).getTrackedIpForUser(userId);
        verify(refreshTokenService, times(1)).createOrReuseRefreshToken(userId, deviceId, userAgent);
        verify(jwtService, times(1)).generateToken(user, deviceId);
        verify(jwtService, times(1)).generateJwtCookie(jwtToken);
        verify(refreshTokenService, times(1)).generateRefreshTokenCookie(jwtToken);
    }

    @Test
    @DisplayName("Проверка ответа с пустым списком IP-адресов")
    void testBuildResponseWithCookies_EmptyIPList() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        AuthenticationResponse response = AuthenticationResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(List.of("ROLE_USER"))
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .tokenType("BEARER")
                .deviceId("device-uuid-789")
                .userIPs(List.of())
                .jwtCookie(ResponseCookie.from("jwt", "jwt-token").build())
                .refreshTokenCookie(ResponseCookie.from("refresh", "refresh-token").build())
                .build();

        String message = "Успешный вход";

        // When
        ResponseEntity<AuthenticationResponseDto> result = authenticationResponseBuilder
                .buildResponseWithCookies(response, message);

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());

        AuthenticationResponseDto dto = result.getBody();
        assertNotNull(dto);
        assertEquals(0, dto.getLastIpAddress().size());
    }

    @Test
    @DisplayName("Проверка создания AuthenticationResponse при null deviceId (генерация нового)")
    void testBuildAuthenticationResponse_WithNullDeviceId() {
        // Given
        Long userId = 1L;
        String deviceId = null; // null deviceId
        String jwtToken = "jwt-token-123";
        String refreshTokenValue = "refresh-token-456";
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String newDeviceId = "new-device-uuid"; // generated device ID

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(fr.mossaab.security.enums.Role.USER)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token(refreshTokenValue)
                .deviceId(newDeviceId) // new generated device ID
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        UserIpTempDto ipDto = UserIpTempDto.builder()
                .ipAddress(clientIp)
                .createdAt(Instant.now())
                .build();

        // Mocking
        when(ipHelper.getClientIp(request)).thenReturn(clientIp);
        doNothing().when(userIpTempService).saveIpTemp(userId, clientIp);
        when(userIpTempService.getTrackedIpForUser(userId)).thenReturn(List.of(ipDto));
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        when(refreshTokenService.createOrReuseRefreshToken(userId, deviceId, userAgent))
                .thenReturn(refreshToken); // returns refreshed token with new deviceId
        when(jwtService.generateToken(user, newDeviceId)).thenReturn(jwtToken);
        when(refreshTokenService.generateRefreshTokenCookie(jwtToken))
                .thenReturn(ResponseCookie.from("refresh", refreshTokenValue).build());
        when(jwtService.generateJwtCookie(jwtToken))
                .thenReturn(ResponseCookie.from("jwt", jwtToken).build());

        // When
        AuthenticationResponse result = authenticationResponseBuilder
                .buildAuthenticationResponse(user, deviceId, request);

        // Then
        assertNotNull(result);
        assertEquals(newDeviceId, result.getDeviceId());
    }
}
