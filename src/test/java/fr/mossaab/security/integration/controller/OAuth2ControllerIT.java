package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.social.SocialExchangeRequest;
import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.social.service.OneTimeAuthCodeService;
import fr.mossaab.security.service.social.service.SocialAccountLinkingService;
import fr.mossaab.security.service.social.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.notNullValue;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OAuth2ControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OneTimeAuthCodeService oneTimeAuthCodeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRegistrationService registration;

    @Autowired
    private SocialAccountLinkingService linkingService;

    @Autowired
    private UserSocialAccountRepository socialAccountRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    // --- Вспомогательные методы ---

    private SocialUserInfo socialInfo(String id, String email) {
        SocialUserInfo info = new SocialUserInfo();
        info.setId(id);
        info.setEmail(email);
        return info;
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .nickname("nick_" + id)
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .temporarySecondsBalance(0)
                .phoneVerified(false)
                .socialAccounts(new HashSet<>())
                .build();
    }

    private RefreshToken refreshToken(Long userId, String deviceId, String token) {
        return RefreshToken.builder()
                .token(token)
                .user(User.builder().id(userId).build())
                .deviceId(deviceId)
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build();
    }

    // ========================================================================
    @Nested
    @DisplayName("/oauth2/social/login")
    class SocialLoginTests {

        @BeforeEach
        public void setUpLogin(){
            userRepository.deleteAll();
        }

        @Test
        @DisplayName("Успешный вход через соцсеть: код валиден, пользователь найден по socialId")
        void loginSuccess() throws Exception {
            OAuthProvider provider = OAuthProvider.GOOGLE;
            String deviceId = "DEVICE2-123";
            String userAgent = "JUnit-Agent";

            User userWithSocial = user(20L, "social2@example.com");

            UserSocialAccount socialAccount = new UserSocialAccount();
            socialAccount.setSocialEmail("social2@example.com");
            socialAccount.setProvider(OAuthProvider.GOOGLE);
            socialAccount.setUser(userWithSocial);
            socialAccount.setExternalId("ID-2102030");

            userWithSocial.setSocialAccounts(new HashSet<>(List.of(socialAccount)));
            userRepository.save(userWithSocial);

            SocialUserInfo info = socialInfo("ID-2102030", "social2@example.com");
            String authCode = oneTimeAuthCodeService.issueCode(info);

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(provider);
            req.setDeviceId(deviceId);

            mockMvc.perform(post("/oauth2/social/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(SecurityMockMvcRequestPostProcessors.user("social2@example.com"))
                            .header("User-Agent", userAgent)
                            .with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, notNullValue()))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.email").value("social2@example.com"))
                    .andExpect(jsonPath("$.message").value("Успешный вход"))
                    .andExpect(jsonPath("$.deviceId").exists());
        }

        @Test
        @DisplayName("Вход через соцсеть: некорректный формат authCode -> 400 и ошибка валидации")
        void loginInvalidCode_formatValidation() throws Exception {
            String authCode = "&?4567-bad-code&"; // заведомо невалидный по Pattern

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(OAuthProvider.GOOGLE);

            mockMvc.perform(post("/oauth2/social/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Данные не прошли валидацию"))
                    .andExpect(jsonPath("$.errors.authCode").value("Некорректный формат auth code"));
        }

        @Test
        @DisplayName("Вход через соцсеть: код не найден в OneTimeAuthCodeService -> 400")
        void loginInvalidCode_service_real() throws Exception {
            String authCode = "123e4567-e89b-12d3-a456-426614174000";

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(OAuthProvider.GOOGLE);
            
            mockMvc.perform(post("/oauth2/social/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Код устарел или неверен"));
        }
    }

    // ========================================================================
// ========================================================================
    @Nested
    @DisplayName("/oauth2/social/register")
    class SocialRegisterTests {

        @Test
        @DisplayName("Успешная регистрация через соцсеть: email не существует, создаётся новый пользователь")
        void registerSuccess() throws Exception {
            OAuthProvider provider = OAuthProvider.VK;
            String deviceId = "DEVICE-REG-1";
            String userAgent = "JUnit-Agent-Reg";

            // Соц. информация, по которой будет происходить регистрация
            SocialUserInfo info = socialInfo("vk-id-1", "newuser@example.com");
            // Реальный код, выдаваемый in‑memory oneTimeAuthCodeService
            String authCode = oneTimeAuthCodeService.issueCode(info);

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(provider);
            req.setDeviceId(deviceId);

            mockMvc.perform(post("/oauth2/social/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .header("User-Agent", userAgent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.message").value("Регистрация успешна"));

            // Дополнительно можно проверить, что пользователь реально создался в БД
            Optional<User> created = userRepository.findByEmail("newuser@example.com");
            assertTrue(created.isPresent(), "Ожидался созданный пользователь newuser@example.com");
        }

        @Test
        @DisplayName("Регистрация через соцсеть: код устарел или неверен -> 400")
        void registerInvalidCode() throws Exception {
            // Код, который не был выдан oneTimeAuthCodeService
            String authCode = "123e4567-e89b-12d3-a456-426614174000";

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(OAuthProvider.VK);

            mockMvc.perform(post("/oauth2/social/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Код устарел или неверен"));
        }

        @Test
        @DisplayName("Регистрация через соцсеть: email уже существует -> 409")
        void registerEmailExists() throws Exception {
            OAuthProvider provider = OAuthProvider.GOOGLE;

            // Сначала создадим пользователя в БД
            User existingUser = user(20L, "exists@example.com");
            userRepository.save(existingUser);

            // Сформируем соц.инфо с тем же email и выпустим реальный код
            SocialUserInfo info = socialInfo("g-id-1", "exists@example.com");
            String authCode = oneTimeAuthCodeService.issueCode(info);

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(provider);

            mockMvc.perform(post("/oauth2/social/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
            // Можно сузить проверку, если знаешь точный текст сообщения
        }
    }

    // ========================================================================
    @Nested
    @DisplayName("/oauth2/social/link")
    class SocialLinkTests {

        @Test
        @WithMockUser(username = "current@example.com")
        @DisplayName("Успешная привязка соцсети к авторизованному пользователю")
        void linkSuccess() throws Exception {
            String deviceId = "DEVICE-LINK-1";
            String userAgent = "JUnit-Agent-Link";
            OAuthProvider provider = OAuthProvider.GOOGLE;

            // Подготовим текущего пользователя в БД
            User currentUser = user(100L, "current@example.com");
            userRepository.save(currentUser);

            // Подготовим соц.инфо с тем же email
            SocialUserInfo info = socialInfo("g-link-id-1", "current@example.com");
            String authCode = oneTimeAuthCodeService.issueCode(info);

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(provider);
            req.setDeviceId(deviceId);

            mockMvc.perform(post("/oauth2/social/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .header("User-Agent", userAgent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.email").value("current@example.com"))
                    .andExpect(jsonPath("$.message").value("Соцсеть привязана"));

            // Дополнительно можно проверить, что у пользователя появилась соц.учётка
            User reloaded = userRepository.findByEmail("current@example.com").orElseThrow();
            assertFalse(reloaded.getSocialAccounts().isEmpty(), "Ожидалась привязанная соц.учётка");
        }

        @Test
        @DisplayName("Привязка без авторизации -> 401")
        void linkUnauthorized() throws Exception {
            OAuthProvider provider = OAuthProvider.GOOGLE;

            SocialUserInfo info = socialInfo("id-unauth", "unauth@example.com");
            String authCode = oneTimeAuthCodeService.issueCode(info);

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(provider);

            // Без @WithMockUser -> контроллер должен вернуть 401
            mockMvc.perform(post("/oauth2/social/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "missing@example.com")
        @DisplayName("Привязка: авторизованный пользователь не найден в БД -> 403")
        void linkCurrentUserNotFound() throws Exception {
            OAuthProvider provider = OAuthProvider.VK;

            // Пользователя в БД НЕ создаём
            SocialUserInfo info = socialInfo("vk-link-id", "missing@example.com");
            String authCode = oneTimeAuthCodeService.issueCode(info);

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(provider);

            mockMvc.perform(post("/oauth2/social/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "current@example.com")
        @DisplayName("Привязка: код устарел или неверен -> 400")
        void linkInvalidCode() throws Exception {
            // Код, который не выдавался oneTimeAuthCodeService
            String authCode = "123e4567-e89b-12d3-a456-426614174000";

            SocialExchangeRequest req = new SocialExchangeRequest();
            req.setAuthCode(authCode);
            req.setProvider(OAuthProvider.GOOGLE);

            mockMvc.perform(post("/oauth2/social/link")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Код устарел или неверен"));
        }
    }
}