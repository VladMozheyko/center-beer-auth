package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.service.social.service.OAuthUserInfoService;
import fr.mossaab.security.service.social.service.SocialUserFlowService;
import fr.mossaab.security.service.social.service.VkTokenService;
import fr.mossaab.security.service.social.service.SocialUserFlowService.SocialAuthResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для VkIdPKCEController.
 *
 * Поднимается веб-контекст, используем MockMvc и мокаем внешние сервисы.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VkIdPKCEControllerIT extends AbstractIntegrationTest{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VkTokenService vkTokenService;

    @MockBean
    private OAuthUserInfoService userInfoService;

    @MockBean
    private SocialUserFlowService flowService;

    // ==========================
    // POST /oauth2/vk_pkce_token
    // ==========================

    @Nested
    @DisplayName("POST /oauth2/vk_pkce_token")
    class VkPkceTokenTests {

        @Test
        @DisplayName("успешный обмен кода на токен и анализ пользователя (200 OK)")
        void vkPkceToken_success() throws Exception {
            // given
            String code = "auth_code_from_vk";
            String codeVerifier = "strong_pkce_verifier_abc123";
            String deviceId = "android_789";

            Map<String, String> requestBody = Map.of(
                    "code", code,
                    "code_verifier", codeVerifier,
                    "device_id", deviceId
            );

            String accessToken = "vk_access_token_123";
            when(vkTokenService.exchangeCodeForToken(code, codeVerifier, deviceId))
                    .thenReturn(accessToken);

            SocialUserInfo userInfo = new SocialUserInfo();
            userInfo.setId("123456789");
            userInfo.setEmail("user@example.com");
            userInfo.setFirstName("Иван");
            userInfo.setLastName("Иванов");

            when(userInfoService.getUserInfo(null, accessToken, OAuthProvider.VK))
                    .thenReturn(userInfo);

            SocialAuthResult authResult = new SocialAuthResult();
            authResult.setStatus(SocialAuthStatus.NEW_ACCOUNT);
            authResult.setMessage("Пользователь не найден. Можно продолжить регистрацию через соцсеть.");
            authResult.setBaseUserEmail(null);
            authResult.setSocialUser(userInfo);
            authResult.setAuthCode("some-auth-code-uuid");

            when(flowService.analyzeUser(userInfo, OAuthProvider.VK))
                    .thenReturn(authResult);

            String responseJson = mockMvc.perform(post("/oauth2/vk_pkce_token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(SocialAuthStatus.NEW_ACCOUNT.name()))
                    .andExpect(jsonPath("$.message").value("Пользователь не найден. Можно продолжить регистрацию через соцсеть."))
                    .andExpect(jsonPath("$.socialUser.id").value("123456789"))
                    .andExpect(jsonPath("$.socialUser.email").value("user@example.com"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            SocialAuthResult response = objectMapper.readValue(responseJson, SocialAuthResult.class);
            assertThat(response.getStatus().name()).isEqualTo(SocialAuthStatus.NEW_ACCOUNT.name());
            assertThat(response.getSocialUser().getEmail()).isEqualTo("user@example.com");

            verify(vkTokenService).exchangeCodeForToken(code, codeVerifier, deviceId);
            verify(userInfoService).getUserInfo(null, accessToken, OAuthProvider.VK);
            verify(flowService).analyzeUser(userInfo, OAuthProvider.VK);
            verifyNoMoreInteractions(vkTokenService, userInfoService, flowService);
        }

        @Test
        @DisplayName("отсутствует code или code_verifier → 400 Bad Request")
        void vkPkceToken_missingCodeOrVerifier() throws Exception {
            // вариант 1: нет code
            Map<String, String> noCode = Map.of(
                    "code_verifier", "verifier"
            );

            mockMvc.perform(post("/oauth2/vk_pkce_token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(noCode)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Missing 'code' or 'code_verifier'"));

            // вариант 2: нет code_verifier
            Map<String, String> noVerifier = Map.of(
                    "code", "some-code"
            );

            mockMvc.perform(post("/oauth2/vk_pkce_token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(noVerifier)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Missing 'code' or 'code_verifier'"));

            verifyNoInteractions(vkTokenService, userInfoService, flowService);
        }

        @Test
        @DisplayName("ошибка в vkTokenService / userInfoService / flowService → 500 с сообщением")
        void vkPkceToken_internalError() throws Exception {
            String code = "bad_code";
            String codeVerifier = "verifier";
            Map<String, String> body = Map.of(
                    "code", code,
                    "code_verifier", codeVerifier
            );

            when(vkTokenService.exchangeCodeForToken(eq(code), eq(codeVerifier), isNull()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            mockMvc.perform(post("/oauth2/vk_pkce_token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Authentication failed: Connection timeout"));

            verify(vkTokenService).exchangeCodeForToken(eq(code), eq(codeVerifier), isNull());
            verifyNoMoreInteractions(vkTokenService);
            verifyNoInteractions(userInfoService, flowService);
        }
    }

    // ==========================
    // GET /oauth2/code/vk
    // ==========================

    @Nested
    @DisplayName("GET /oauth2/code/vk")
    class VkRedirectProxyTests {

        @Test
        @DisplayName("успешное перенаправление на фронтенд (302 Found)")
        void vkRedirectProxy_success() throws Exception {
            String code = "abcd1234xyz";
            String state = "secure_state_5678";
            String deviceId = "mobile_001";

            String expectedRedirectUrl = "https://frontend.app?code=" + code + "&state=" + state + "&device_id=" + deviceId;

            when(vkTokenService.buildFrontendRedirectUrl(code, state, deviceId))
                    .thenReturn(expectedRedirectUrl);

            mockMvc.perform(get("/oauth2/code/vk")
                            .param("code", code)
                            .param("state", state)
                            .param("device_id", deviceId))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", expectedRedirectUrl));

            verify(vkTokenService).buildFrontendRedirectUrl(code, state, deviceId);
            verifyNoMoreInteractions(vkTokenService);
        }

        @Test
        @DisplayName("перенаправление с отсутствующим device_id (null передаётся в сервис)")
        void vkRedirectProxy_withoutDeviceId() throws Exception {
            String code = "abcd1234xyz";
            String state = "secure_state_5678";

            String expectedRedirectUrl = "https://frontend.app?code=" + code + "&state=" + state;

            when(vkTokenService.buildFrontendRedirectUrl(eq(code), eq(state), isNull()))
                    .thenReturn(expectedRedirectUrl);

            mockMvc.perform(get("/oauth2/code/vk")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", expectedRedirectUrl));

            verify(vkTokenService).buildFrontendRedirectUrl(eq(code), eq(state), isNull());
            verifyNoMoreInteractions(vkTokenService);
        }

        @Test
        @DisplayName("IOException при sendRedirect пробрасывается как 500 (Spring обернёт), но мы проверяем, что сервис вызван")
        void vkRedirectProxy_ioException() throws Exception {
            String code = "code";
            String state = "state";
            String deviceId = "dev";

            when(vkTokenService.buildFrontendRedirectUrl(code, state, deviceId))
                    .thenReturn("invalid://url");

            mockMvc.perform(get("/oauth2/code/vk")
                            .param("code", code)
                            .param("state", state)
                            .param("device_id", deviceId))
                    .andExpect(status().isFound());

            verify(vkTokenService).buildFrontendRedirectUrl(code, state, deviceId);
        }
    }
}
