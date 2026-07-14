package fr.mossaab.security.unit.service;

import fr.mossaab.security.builder.AuthenticationResponseBuilder;
import fr.mossaab.security.dto.SessionInfoResponse;
import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.RegisterRequest;
import fr.mossaab.security.dto.auth.ResetPasswordRequest;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.repository.UserRepository;

import fr.mossaab.security.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests сервиса AuthenticationService")
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private MailSender mailSender;

    @Mock
    private UserIpTempService userIpTempService;

    @Mock
    private IpHelper helper;

    @Mock
    AuthenticationResponseBuilder authResponseBuilder;

    @BeforeEach
    void setUp() {
        // Mock static values from @Value fields
        ReflectionTestUtils.setField(authenticationService, "publicUrl", "https://api.center.beer/auth_service");
    }

    @Test
    @DisplayName("Регистрация нового пользователя")
    void register_NewUser_Success() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password", "testNickname");
        User savedUser = User.builder()
                .id(1L)
                .role(Role.USER)
                .email(request.getEmail())
                .nickname(request.getNickname())
                .build();

        // Настройка mock объектов
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(request.getNickname())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        // Stub getClientIp to return 127.0.0.1 for the mock request
        when(helper.getClientIp(mockRequest)).thenReturn("127.0.0.1");
        // Stub saveIpTemp to do nothing (we'll verify it was called with correct params)
        doNothing().when(userIpTempService).saveIpTemp(anyLong(), anyString());
        when(authResponseBuilder.getIpHelper()).thenReturn(helper);
        when(helper.getClientIp(mockRequest)).thenReturn("127.0.0.1");

        // Вызов метода
        authenticationService.register(request, mockRequest);

        // Проверка результата
        verify(mailSender, times(1)).send(eq(request.getEmail()), eq("Ссылка активации CENTER.BEER"), anyString());
        verify(userIpTempService, times(1)).saveIpTemp(eq(1L), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Ошибка регистрации из-за существующего email")
    void register_ExistingEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password", "testNickname");

        User existingUser = User.builder().email(request.getEmail()).activationCode(null).build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> authenticationService.register(request, mockRequest));

        assertEquals("Пользователь с таким email уже существует и активирован.", exception.getMessage());
    }

    @Test
    @DisplayName("Восстановление пароля - пользователь существует")
    void requestPasswordReset_UserExists() {
        String email = "test@example.com";
        User user = User.builder().email(email).nickname("testUser").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        authenticationService.requestPasswordReset(email);

        assertNotNull(user.getActivationCode());
        verify(userRepository, times(1)).save(user);
        verify(mailSender, times(1)).send(eq(email), eq("Код для смены пароля"), anyString());
    }

    @Test
    @DisplayName("Восстановление пароля - пользователь не найден")
    void requestPasswordReset_UserNotFound() {
        String email = "notfound@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authenticationService.requestPasswordReset(email));
    }

    @Test
    @DisplayName("Аутентификация: успешная авторизация")
    void authenticate_Success() {
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password", "ABCD-DCBA-1234");

        User user = User.builder()
                .email(request.getEmail())
                .role(Role.USER)
                .activationCode(null)
                .build();

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        // Настройка моков
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                .accessToken("jwtToken")
                .email("test@example.com")
                .refreshToken("refreshToken")
                .jwtCookie(ResponseCookie.from("jwt", "jwtValue").build())
                .refreshTokenCookie(ResponseCookie.from("refreshToken", "refreshTokenValue").build())
                .deviceId("ABCD-DCBA-1234")
                .userIPs(new ArrayList<>())
                .build();
        when(authResponseBuilder.buildAuthenticationResponse(any(User.class), eq("ABCD-DCBA-1234"), eq(mockRequest))).thenReturn(mockResponse);
        
        // Выполнение тестируемого метода
        AuthenticationResponse response = authenticationService.authenticate(request, mockRequest);

        // Проверка ответа
        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
        assertEquals(user.getEmail(), response.getEmail());

        // Проверка вызовов
        verify(authenticationManager, times(1)).authenticate(any());
        verify(userRepository, times(1)).findByEmail(request.getEmail());
    }

    @Test
    @DisplayName("Активировать пользователя: успех")
    void activateUser_Success() {
        String code = "activationCode";
        User user = User.builder().activationCode(code).build();

        when(userRepository.findByActivationCode(code)).thenReturn(Optional.of(user));

        boolean isActivated = authenticationService.activateUser(code);

        assertTrue(isActivated);
        assertNull(user.getActivationCode());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Активировать пользователя: код не найден")
    void activateUser_CodeNotFound() {
        String code = "activationCode";

        when(userRepository.findByActivationCode(code)).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class, () -> authenticationService.activateUser(code));
    }

    @Test
    @DisplayName("Сброс пароля: пароли не совпадают")
    void resetPassword_PasswordsDoNotMatch() {
        ResetPasswordRequest request = new ResetPasswordRequest("code", "newPassword", "wrongRepeat");

        ResponseEntity<Object> response = authenticationService.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Пароли не совпадают", response.getBody());
    }

    @Test
    @DisplayName("Сброс пароля: успешный сброс")
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest("code", "newPassword", "newPassword");
        User user = User.builder().activationCode(request.getCode()).build();

        when(userRepository.findByActivationCode(request.getCode())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("encodedPassword");

        ResponseEntity<Object> response = authenticationService.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Пароль успешно изменён", response.getBody());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Сброс пароля: код не найден")
    void resetPassword_CodeNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest("code", "newPassword", "newPassword");

        when(userRepository.findByActivationCode(request.getCode())).thenReturn(Optional.empty());

        ResponseEntity<Object> response = authenticationService.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Неверный или просроченный код", response.getBody());
    }

    @Test
    @DisplayName("Отправка повторной активации")
    void resendActivationCode_Success() {
        String email = "test@example.com";
        User user = User.builder().email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        authenticationService.resendActivationCode(email);

        assertNotNull(user.getActivationCode());
        verify(mailSender, times(1)).send(eq(email), eq("Ссылка активации CENTER.BEER"), anyString());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Выход из системы")
    void logout_Success() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String refreshTokenValue = "refreshToken";

        // Мокаем методы для получения и удаления refresh токена
        when(refreshTokenService.getRefreshTokenFromCookies(request)).thenReturn(refreshTokenValue);
        doNothing().when(refreshTokenService).deleteByToken(refreshTokenValue);

        // Мокаем методы генерации пустых кук
        ResponseCookie jwtCookieMock = ResponseCookie.from("jwt", "").build();
        ResponseCookie refreshTokenCookieMock = ResponseCookie.from("refreshToken", "").build();

        when(jwtService.getCleanJwtCookie()).thenReturn(jwtCookieMock);
        when(refreshTokenService.getCleanRefreshTokenCookie()).thenReturn(refreshTokenCookieMock);

        // Выполнение тестируемого метода
        ResponseEntity<Void> entity = authenticationService.logout(request);

        // Проверка
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(refreshTokenService, times(1)).deleteByToken(refreshTokenValue);
        assertTrue(Objects.requireNonNull(entity.getHeaders().get(HttpHeaders.SET_COOKIE)).contains(jwtCookieMock.toString()));
        assertTrue(Objects.requireNonNull(entity.getHeaders().get(HttpHeaders.SET_COOKIE)).contains(refreshTokenCookieMock.toString()));
    }

    // Получение активных сессий
    @Nested
    class ActiveSession {

        private final AutoCloseable closeable = MockitoAnnotations.openMocks(this);

        @AfterEach
        void tearDown() throws Exception {
            SecurityContextHolder.clearContext();
            closeable.close();
        }

        @Test
        @DisplayName("getActiveSessions: UNAUTHORIZED если нет аутентификации")
        void getActiveSessions_NoAuthentication_Unauthorized() {
            SecurityContextHolder.clearContext(); // authentication = null

            ResponseEntity<List<SessionInfoResponse>> response = authenticationService.getActiveSessions();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();

            verifyNoInteractions(userRepository, refreshTokenService);
        }

        @Test
        @DisplayName("getActiveSessions: UNAUTHORIZED если principal не UserDetails")
        void getActiveSessions_PrincipalNotUserDetails_Unauthorized() {
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("rawPrincipalString", "password");
            SecurityContextHolder.getContext().setAuthentication(auth);

            ResponseEntity<List<SessionInfoResponse>> response = authenticationService.getActiveSessions();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();

            verifyNoInteractions(userRepository, refreshTokenService);
        }

        @Test
        @DisplayName("getActiveSessions: OK и корректное наполнение списка сессий")
        void getActiveSessions_Success() {
            // given
            String email = "user@example.com";

            UserDetails springUserDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(springUserDetails, "password", "ROLE_USER");
            SecurityContextHolder.getContext().setAuthentication(auth);

            Long userId = 1L;
            User userEntity =
                    User.builder()
                            .id(userId)
                            .email(email)
                            .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

            Instant now = Instant.now();
            RefreshToken rt1 = RefreshToken.builder()
                    .id(10L)
                    .token("token-1")
                    .expiryDate(now.plusSeconds(3600))
                    .revoked(false)
                    .createdAt(now.minusSeconds(1000))
                    .lastUsedAt(now.minusSeconds(100))
                    .deviceInfo("AndroidX")
                    .build();

            RefreshToken rt2 = RefreshToken.builder()
                    .id(11L)
                    .token("token-2")
                    .expiryDate(now.plusSeconds(7200))
                    .revoked(true)
                    .createdAt(now.minusSeconds(2000))
                    .lastUsedAt(now.minusSeconds(200))
                    .deviceInfo("Chrome")
                    .build();

            when(refreshTokenService.getAllByUserId(userId)).thenReturn(List.of(rt1, rt2));

            // when
            ResponseEntity<List<SessionInfoResponse>> response = authenticationService.getActiveSessions();

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            SessionInfoResponse s1 = Objects.requireNonNull(response.getBody()).get(0);
            SessionInfoResponse s2 = response.getBody().get(1);

            assertThat(s1.getId()).isEqualTo(rt1.getId());
            assertThat(s1.getToken()).isEqualTo(rt1.getToken());
            assertThat(s1.getExpiryDate()).isEqualTo(rt1.getExpiryDate());
            assertThat(s1.isRevoked()).isEqualTo(rt1.isRevoked());
            assertThat(s1.getCreatedAt()).isEqualTo(rt1.getCreatedAt());
            assertThat(s1.getLastUsedAt()).isEqualTo(rt1.getLastUsedAt());
            assertThat(s1.getDeviceInfo()).isEqualTo(rt1.getDeviceInfo());

            assertThat(s2.getId()).isEqualTo(rt2.getId());
            assertThat(s2.getToken()).isEqualTo(rt2.getToken());
            assertThat(s2.getExpiryDate()).isEqualTo(rt2.getExpiryDate());
            assertThat(s2.isRevoked()).isEqualTo(rt2.isRevoked());
            assertThat(s2.getCreatedAt()).isEqualTo(rt2.getCreatedAt());
            assertThat(s2.getLastUsedAt()).isEqualTo(rt2.getLastUsedAt());
            assertThat(s2.getDeviceInfo()).isEqualTo(rt2.getDeviceInfo());

            verify(userRepository, times(1)).findByEmail(email);
            verify(refreshTokenService, times(1)).getAllByUserId(userId);
        }
    }

    @Nested
    class LogoutAllDevices {

        @Mock
        private HttpServletRequest httpServletRequest;

        private AutoCloseable closeable;

        @BeforeEach
        void setUp() {
            closeable = MockitoAnnotations.openMocks(this);
            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void tearDown() throws Exception {
            SecurityContextHolder.clearContext();
            closeable.close();
        }

        @Test
        @DisplayName("logoutAllDevices: UNAUTHORIZED если нет аутентификации")
        void logoutAllDevices_NoAuthentication_Unauthorized() {
            SecurityContextHolder.clearContext(); // authentication = null

            ResponseEntity<Void> response = authenticationService.logoutAllDevices(httpServletRequest, true);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();

            verifyNoInteractions(userRepository, refreshTokenService);
        }

        @Test
        @DisplayName("logoutAllDevices: UNAUTHORIZED если principal не UserDetails")
        void logoutAllDevices_PrincipalNotUserDetails_Unauthorized() {
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("rawPrincipal", "password");
            SecurityContextHolder.getContext().setAuthentication(auth);

            ResponseEntity<Void> response = authenticationService.logoutAllDevices(httpServletRequest, true);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();

            verifyNoInteractions(userRepository, refreshTokenService);
        }

        @Test
        @DisplayName("logoutAllDevices: isExitThisDevice = true -> удаляются все токены пользователя и выставляется очищающий cookie")
        void logoutAllDevices_ExitThisDeviceTrue_Success() {
            // given
            String email = "user@example.com";
            UserDetails springUserDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("pwd")
                    .authorities("ROLE_USER")
                    .build();

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(springUserDetails, "pwd", "ROLE_USER");
            SecurityContextHolder.getContext().setAuthentication(auth);

            Long userId = 1L;
            User user = User.builder()
                    .id(userId)
                    .email(email)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
                    .path("/")
                    .maxAge(0)
                    .build();
            when(refreshTokenService.getCleanRefreshTokenCookie()).thenReturn(cleanCookie);

            // when
            ResponseEntity<Void> response = authenticationService.logoutAllDevices(httpServletRequest, true);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                    .isEqualTo(cleanCookie.toString());

            verify(userRepository, times(1)).findByEmail(email);
            verify(refreshTokenService, times(1)).deleteAllByUserId(userId);
            verify(refreshTokenService, never()).getRefreshTokenFromCookies(any());
            verify(refreshTokenService, never()).deleteEverythingExceptTheCurrentDevice(anyString(), anyLong());
        }

        @Test
        @DisplayName("logoutAllDevices: isExitThisDevice = false -> удаляются все сессии кроме текущей")
        void logoutAllDevices_ExitThisDeviceFalse_Success() {
            // given
            String email = "user2@example.com";
            UserDetails springUserDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("pwd")
                    .authorities("ROLE_USER")
                    .build();

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(springUserDetails, "pwd", "ROLE_USER");
            SecurityContextHolder.getContext().setAuthentication(auth);

            Long userId = 2L;
            User user = User.builder()
                    .id(userId)
                    .email(email)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            String currentRefreshToken = "current-refresh-token";
            when(refreshTokenService.getRefreshTokenFromCookies(httpServletRequest))
                    .thenReturn(currentRefreshToken);

            // when
            ResponseEntity<Void> response = authenticationService.logoutAllDevices(httpServletRequest, false);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNull();
            // В этом сценарии cookie не выставляется
            assertThat(response.getHeaders().containsKey(HttpHeaders.SET_COOKIE)).isFalse();

            verify(userRepository, times(1)).findByEmail(email);
            verify(refreshTokenService, times(1))
                    .getRefreshTokenFromCookies(httpServletRequest);
            verify(refreshTokenService, times(1))
                    .deleteEverythingExceptTheCurrentDevice(currentRefreshToken, userId);

            verify(refreshTokenService, never()).deleteAllByUserId(anyLong());
            verify(refreshTokenService, never()).getCleanRefreshTokenCookie();
        }
    }
}