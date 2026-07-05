package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.auth.*;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.RegistrationMethod;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private PhoneRegistrationFacade phoneRegistrationFacade;

    @MockBean
    private StorageService storageService;

    @MockBean
    private PhoneVerificationService phoneVerificationService;

    @MockBean
    private MailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        Mockito.when(phoneVerificationService.sendCode(anyString(), any())).thenReturn("1234");
    }

    @Test
    @DisplayName("Получить пользователя по идентификатору")
    void getUserById_ShouldReturnUserProfile() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setNickname("testuser");
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        mockMvc.perform(get("/authentication/by-id/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.nickname").value(user.getNickname()));
    }

    @Test
    @DisplayName("Регистрация пользователя по телефону")
    void registerByPhone_ShouldSendCode() throws Exception {
        PhoneRegisterRequest request = new PhoneRegisterRequest();
        request.setEmail("test@example.com");
        request.setNickname("testuser");
        request.setPhone("+71234567890");
        request.setPassword("securepassword3");
        request.setMethod(RegistrationMethod.CALL);

        mockMvc.perform(post("/authentication/register-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Код отправлен"));

        ArgumentCaptor<PhoneRegisterRequest> captor = ArgumentCaptor.forClass(PhoneRegisterRequest.class);
        Mockito.verify(phoneRegistrationFacade).start(captor.capture(), any(HttpServletRequest.class));
        assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Вход пользователя (проверка тела ответа и статуса)")
    void authenticate_ShouldLoginUser() throws Exception {
        User user = new User();
        user.setCreatedAt(LocalDateTime.now());
        user.setEmail("loginuser2@example.com");
        user.setNickname("loginuser2");
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode("loginpassword45"));
        userRepository.save(user);

        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("loginuser2@example.com");
        authRequest.setPassword("loginpassword45");

        AuthenticationResponse mockAuthResponse = AuthenticationResponse.builder()
                .accessToken("mocked-access-token")
                .refreshToken("mocked-refresh-token")
                .deviceId("device-123")
                .jwtCookie(ResponseCookie.from("jwt-cookie", "mocked-access-token").build())
                .refreshTokenCookie(ResponseCookie.from("refresh-jwt-cookie", "mocked-refresh-token").build())
                .build();

        when(authenticationService.authenticate(any(AuthenticationRequest.class), any(HttpServletRequest.class)))
                .thenReturn(mockAuthResponse);

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit Test Agent")
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Успешная аутентификация через логин и пароль"))
                .andExpect(jsonPath("$.accessToken").value("mocked-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"))
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.status").value(String.valueOf(HttpStatus.OK.value())));
    }

    @Test
    @DisplayName("Подтверждение телефона")
    void confirmPhone_ShouldConfirmNumber() throws Exception {
        User user = new User();
        user.setCreatedAt(LocalDateTime.now());
        user.setEmail("loginuser3@example.com");
        user.setNickname("loginuser3");
        user.setPhone("+71234567890");
        user.setPhoneActivationCode("1234");
        user = userRepository.save(user);

        ConfirmPhoneRequest confirmRequest = new ConfirmPhoneRequest();
        confirmRequest.setPhone("+71234567890");
        confirmRequest.setCode("1234");

        mockMvc.perform(post("/authentication/confirm-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Телефон подтверждён"));

        Mockito.verify(phoneRegistrationFacade).confirm("+71234567890", "1234");
    }

    @Test
    @DisplayName("Запрос на сброс пароля")
    void requestPasswordReset_ShouldSendCode() throws Exception {
        User user = new User();
        user.setCreatedAt(LocalDateTime.now());
        user.setEmail("resetuser@example.com");
        user.setNickname("resetuser");
        user = userRepository.save(user);

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmail("resetuser@example.com");

        Mockito.doNothing().when(mailSender).send(anyString(), anyString(), anyString());

        mockMvc.perform(post("/authentication/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Код для смены пароля успешно отправлен"));

        Mockito.verify(authenticationService).requestPasswordReset("resetuser@example.com");
    }

    @Test
    @DisplayName("Активация пользователя через код")
    void activateUser_ShouldActivateUser() throws Exception {
        User user = new User();
        user.setEmail("activateuser@example.com");
        user.setNickname("activateuser");
        user.setActivationCode("activate123");
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        Mockito.when(authenticationService.activateUser(user.getActivationCode()))
                .thenReturn(true);

        mockMvc.perform(get("/authentication/activate/" + user.getActivationCode()))
                .andExpect(status().isOk())
                .andExpect(content().string("Пользователь успешно зарегистрирован"));

        Mockito.verify(authenticationService).activateUser(user.getActivationCode());
    }

    @Test
    @DisplayName("Отправить повторный код активации")
    void resendActivationCode_ShouldResendCode() throws Exception {
        User user = new User();
        user.setEmail("resend@example.com");
        user.setNickname("resenduser");
        user.setActivationCode(UUID.randomUUID().toString());
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setEmail(user.getEmail());

        mockMvc.perform(post("/authentication/resend-activation-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Код подтверждения аккаунта успешно отправлен на почту"));

        Mockito.verify(authenticationService).resendActivationCode(user.getEmail());
    }

    @Test
    @DisplayName("Обновление токена через куки")
    void refreshTokenCookie_ShouldRefreshToken() throws Exception {
        when(authenticationService.refreshTokenUsingCookie(any(HttpServletRequest.class)))
                .thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(post("/authentication/refresh-token-cookie")
                        .cookie(new Cookie("refresh-jwt-cookie", "some-refresh-token")))
                .andExpect(status().isOk());

        Mockito.verify(authenticationService).refreshTokenUsingCookie(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Обновление токена по телу запроса")
    void refreshToken_ShouldGenerateNewToken() throws Exception {
        String validBase64RefreshToken = "dGVzdC10b2tlbg==";

        AuthenticationService.RefreshTokenRequest req =
                new AuthenticationService.RefreshTokenRequest();
        req.setRefreshToken(validBase64RefreshToken);

        AuthenticationService.RefreshTokenResponse resp =
                AuthenticationService.RefreshTokenResponse.builder()
                        .accessToken("new-access-token")
                        .refreshToken("new-refresh-token")
                        .tokenType("Bearer")
                        .build();

        doReturn(resp).when(refreshTokenService)
                .generateNewToken(any(AuthenticationService.RefreshTokenRequest.class));

        mockMvc.perform(post("/authentication/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("new-refresh-token"));
    }

    @Test
    @DisplayName("Выход из системы")
    void logout_ShouldLogoutUser() throws Exception {
        when(authenticationService.logout(any(HttpServletRequest.class)))
                .thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(post("/authentication/logout")
                        .cookie(new Cookie("refresh-jwt-cookie", "existingToken")))
                .andExpect(status().isOk());

        Mockito.verify(authenticationService).logout(any(HttpServletRequest.class));
    }
}