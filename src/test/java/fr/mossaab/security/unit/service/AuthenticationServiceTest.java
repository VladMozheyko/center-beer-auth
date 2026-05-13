package fr.mossaab.security.unit.service;


import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.RegisterRequest;
import fr.mossaab.security.dto.auth.ResetPasswordRequest;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.UserRepository;

import fr.mossaab.security.service.AuthenticationService;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

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

        // Создаем мок для нового клиента RefreshToken
        RefreshToken mockRefreshToken = RefreshToken.builder()
                .token("mockRefreshToken")
                .build();

        // Настройка mock объектов
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(request.getNickname())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(refreshTokenService.createRefreshToken(savedUser.getId())).thenReturn(mockRefreshToken);

        // Вызов метода
        AuthenticationResponse response = authenticationService.register(request);

        // Проверка результата
        verify(mailSender, times(1)).send(eq(request.getEmail()), eq("Ссылка активации CENTER.BEER"), anyString());
        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
    }

    @Test
    @DisplayName("Ошибка регистрации из-за существующего email")
    void register_ExistingEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password", "testNickname");

        User existingUser = User.builder().email(request.getEmail()).activationCode(null).build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> authenticationService.register(request));

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
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");

        User user = User.builder()
                .email(request.getEmail())
                .role(Role.USER)
                .activationCode(null)
                .build();

        RefreshToken refreshToken = new RefreshToken(1L, user, "refreshToken",
                Instant.now().plusSeconds(60 * 60 * 24), true);

        // Mock response cookies
        ResponseCookie jwtCookieMock = ResponseCookie.from("jwt", "jwtValue").build();
        ResponseCookie refreshTokenCookieMock = ResponseCookie.from("refreshToken", "refreshTokenValue").build();

        when(jwtService.generateJwtCookie(anyString())).thenReturn(jwtCookieMock);
        when(refreshTokenService.generateRefreshTokenCookie(anyString())).thenReturn(refreshTokenCookieMock);

        // Настройка моков
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwtToken");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

        // Создаем mock для возвращаемого объекта аутентификации
        Authentication auth = Mockito.mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        // Выполнение тестируемого метода
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Проверка ответа
        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
        assertEquals(user.getEmail(), response.getEmail());

        // Проверка вызовов
        verify(authenticationManager, times(1)).authenticate(any());
        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, times(1)).generateToken(user);
        verify(refreshTokenService, times(1)).createRefreshToken(user.getId());

        // Проверка вернувшихся кук
        assertEquals(jwtCookieMock.toString(), response.getJwtCookie());
        assertEquals(refreshTokenCookieMock.toString(), response.getRefreshTokenCookie());
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
    void resendActivationCode_Success() throws Exception {
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
        assertTrue(entity.getHeaders().get(HttpHeaders.SET_COOKIE).contains(jwtCookieMock.toString()));
        assertTrue(entity.getHeaders().get(HttpHeaders.SET_COOKIE).contains(refreshTokenCookieMock.toString()));
    }
}