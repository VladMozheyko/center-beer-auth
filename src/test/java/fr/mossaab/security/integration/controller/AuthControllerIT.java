package fr.mossaab.security.integration.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.auth.*;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.RegistrationMethod;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private PhoneVerificationService phoneVerificationService;
    @MockBean
    private MailSender mailSender;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
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
    }

    @Test
    @DisplayName("Вход пользователя")
    void authenticate_ShouldLoginUser() throws Exception {
        User user = new User();
        user.setCreatedAt(LocalDateTime.now());
        user.setEmail("loginuser2@example.com");
        user.setNickname("loginuser2");
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode("loginpassword45")); // Задаем закодированный пароль
        userRepository.save(user);

        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("loginuser2@example.com");
        authRequest.setPassword("loginpassword45");

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Вход в систему пользователя успешно совершен"));
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

        Mockito.doNothing().when(mailSender);

        mockMvc.perform(post("/authentication/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Код для смены пароля успешно отправлен"));
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

        mockMvc.perform(get("/authentication/activate/" + user.getActivationCode()))
                .andExpect(status().isOk())
                .andExpect(content().string("Пользователь успешно зарегистрирован"));
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
    }

    @Test
    @DisplayName("Обновление токена через куки")
    void refreshTokenCookie_ShouldRefreshToken() throws Exception {
        User user = new User();
        user.setEmail("resend@example.com");
        user.setNickname("resenduser");
        user.setActivationCode(UUID.randomUUID().toString());
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.USER);
        user = userRepository.save(user);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        mockMvc.perform(post("/authentication/refresh-token-cookie")
                        .cookie(new Cookie("refresh-jwt-cookie", refreshToken.getToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Выход из системы")
    void logout_ShouldLogoutUser() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("refreshToken", "existingToken")});

        mockMvc.perform(post("/authentication/logout"))
                .andExpect(status().isOk());
    }
}