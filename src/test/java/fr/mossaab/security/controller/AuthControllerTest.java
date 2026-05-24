package fr.mossaab.security.controller;

import fr.mossaab.security.dto.auth.*;
import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.RegistrationMethod;
import fr.mossaab.security.service.AuthenticationService;
import fr.mossaab.security.service.PhoneRegistrationFacade;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.StorageService;
import fr.mossaab.security.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests for AuthController")
class AuthControllerTest {

    @InjectMocks
    private AuthController authController; // Тестируемый контроллер

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private StorageService storageService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PhoneRegistrationFacade phoneRegistrationFacade;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("Получение пользователя по id - успешное")
    void getUserById_Success() {
        User user = User.builder().id(1L).email("test@example.com").nickname("testUser").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<UserProfileResponse> response = authController.getUserById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("testUser", Objects.requireNonNull(response.getBody()).getNickname());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Регистрация по телефону - успешная")
    void registerByPhone_Success() {
        PhoneRegisterRequest request = PhoneRegisterRequest.builder()
                .email("email@example.com")
                .nickname("nickname")
                .password("password")
                .phone("123456789")
                .method(RegistrationMethod.SMS)
                .build();

        ResponseEntity<?> response = authController.registerByPhone(request);

        assertEquals(200, response.getStatusCode().value());
        verify(phoneRegistrationFacade, times(1)).start(request);
    }

    @Test
    @DisplayName("Подтверждение телефона - успешное")
    void confirmPhone_Success() {
        ConfirmPhoneRequest request = ConfirmPhoneRequest.builder()
                .phone("123456789")
                .code("code")
                .build();

        ResponseEntity<?> response = authController.confirmPhone(request);

        assertEquals(200, response.getStatusCode().value());
        verify(phoneRegistrationFacade, times(1)).confirm("123456789", "code");
    }

    @Test
    @DisplayName("Скачивание PDF из файловой системы - успешное")
    void downloadPdfFromFileSystem_Success() throws IOException {
        when(storageService.downloadImageFromFileSystem("test.pdf")).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<?> response = authController.downloadPdfFromFileSystem("test.pdf");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        verify(storageService, times(1)).downloadImageFromFileSystem("test.pdf");
    }

    @Test
    @DisplayName("Регистрация пользователя - успешная")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("email@example.com", "password", "nickname");
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("AndroidX");
        ResponseEntity<Object> response = authController.register(request, httpServletRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(authenticationService, times(1)).register(request, "AndroidX");
    }

    @Test
    @DisplayName("Активация пользователя - успешная")
    void activateUser_Success() {
        ResponseEntity<Object> response = authController.activateUser("activation-code");

        assertEquals(200, response.getStatusCode().value());
        verify(authenticationService, times(1)).activateUser("activation-code");
    }

    @Test
    @DisplayName("Вход пользователя - успешный")
    void authenticate_Success() {
        AuthenticationRequest authRequest =
                new AuthenticationRequest("email@example.com", "password", "");

        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .accessToken("accessToken")
                .email("email@example.com")
                .refreshToken("refreshToken")
                .tokenType("Bearer")
                .id(1L)
                .roles(List.of("ROLE_USER"))
                .build();

        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("AndroidX");

        when(authenticationService.authenticate(eq(authRequest), anyString()))
                .thenReturn(authResponse);

        ResponseEntity<Object> response =
                authController.authenticate(authRequest, httpServletRequest);

        assertEquals(200, response.getStatusCode().value());

        verify(authenticationService, times(1))
                .authenticate(eq(authRequest), eq("AndroidX"));
    }

    @Test
    @DisplayName("Повторная отправка кода активации - успешная")
    void resendActivationCode_Success() throws ParseException {
        EmailRequest req = new EmailRequest();
        req.setEmail("email@example.com");

        ResponseEntity<Object> response = authController.resendActivationCode(req);

        assertEquals(200, response.getStatusCode().value());
        verify(authenticationService, times(1)).resendActivationCode("email@example.com");
    }

    @Test
    @DisplayName("Обновление токена - успешное")
    void refreshToken_Success() {
        AuthenticationService.RefreshTokenRequest tokenRequest = new AuthenticationService.RefreshTokenRequest("refreshToken");
        AuthenticationService.RefreshTokenResponse tokenResponse = new AuthenticationService.RefreshTokenResponse("accessToken", "refreshToken", "Bearer");

        when(refreshTokenService.generateNewToken(tokenRequest)).thenReturn(tokenResponse);

        ResponseEntity<AuthenticationService.RefreshTokenResponse> response = authController.refreshToken(tokenRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(refreshTokenService, times(1)).generateNewToken(tokenRequest);
    }

    @Test
    @DisplayName("Обновление токена через куки - успешное")
    void refreshTokenCookie_Success() {
        authController.refreshTokenCookie(request);

        verify(authenticationService, times(1)).refreshTokenUsingCookie(request);
    }

    @Test
    @DisplayName("Выход из системы - успешный")
    void logout_Success() {
        authController.logout(request);

        verify(authenticationService, times(1)).logout(request);
    }

    @Test
    @DisplayName("Запрос на смену пароля - успешный")
    void requestPasswordReset_Success() {
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmail("email@example.com");

        ResponseEntity<Object> response = authController.requestPasswordReset(emailRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(authenticationService, times(1)).requestPasswordReset("email@example.com");
    }

    @Test
    @DisplayName("Смена пароля по коду - успешная")
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest("code", "newPassword", "newPassword");
        authController.resetPassword(request);

        verify(authenticationService, times(1)).resetPassword(request);
    }
}