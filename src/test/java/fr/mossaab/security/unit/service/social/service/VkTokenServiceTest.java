package fr.mossaab.security.unit.service.social.service;


import fr.mossaab.security.service.social.service.VkTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для VkTokenService.
 */
@ExtendWith(MockitoExtension.class)
class VkTokenServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VkTokenService vkTokenService;

    @BeforeEach
    void setUp() throws Exception {
        // Подставим значения @Value через reflection
        setField(vkTokenService, "frontendUrl", "https://frontend.example.com/callback");
        setField(vkTokenService, "vkClientId", "test-client-id");
        setField(vkTokenService, "vkRedirectUri", "https://backend.example.com/oauth2/callback/vk");
    }

    // утилита для установки приватных полей с @Value
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // --------- ТЕСТЫ ДЛЯ exchangeCodeForToken ---------

    @Nested
    @DisplayName("exchangeCodeForToken")
    class ExchangeCodeForTokenTests {

        @Test
        @DisplayName("Успешный обмен кода на токен с device_id")
        void exchangeCodeForToken_Success_WithDeviceId() {
            String code = "auth-code";
            String codeVerifier = "code-verifier";
            String deviceId = "device-123";

            // Формируем тело ответа VK
            Map<String, Object> body = new HashMap<>();
            body.put("access_token", "vk-access-token");

            ResponseEntity<Map> responseEntity =
                    new ResponseEntity<>(body, HttpStatus.OK);

            // Мок ответа VK
            when(restTemplate.postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    any(HttpEntity.class),
                    eq(Map.class))
            ).thenReturn(responseEntity);

            // когда
            String token = vkTokenService.exchangeCodeForToken(code, codeVerifier, deviceId);

            // тогда
            assertEquals("vk-access-token", token);

            // Проверяем, что мы отправляли форму с нужными полями
            ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> captor =
                    ArgumentCaptor.forClass(HttpEntity.class);

            verify(restTemplate).postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    captor.capture(),
                    eq(Map.class)
            );

            HttpEntity<MultiValueMap<String, String>> sentRequest = captor.getValue();
            assertNotNull(sentRequest);

            // Проверяем заголовки
            HttpHeaders headers = sentRequest.getHeaders();
            assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());

            // Проверяем параметры формы
            MultiValueMap<String, String> params = sentRequest.getBody();
            assertNotNull(params);
            assertEquals("authorization_code", params.getFirst("grant_type"));
            assertEquals("test-client-id", params.getFirst("client_id"));
            assertEquals("https://backend.example.com/oauth2/callback/vk", params.getFirst("redirect_uri"));
            assertEquals(code, params.getFirst("code"));
            assertEquals(codeVerifier, params.getFirst("code_verifier"));
            assertEquals(deviceId, params.getFirst("device_id"));
        }

        @Test
        @DisplayName("Успешный обмен кода на токен без device_id (deviceId = null)")
        void exchangeCodeForToken_Success_WithoutDeviceId_Null() {
            String code = "auth-code";
            String codeVerifier = "code-verifier";
            String deviceId = null;

            Map<String, Object> body = new HashMap<>();
            body.put("access_token", "vk-access-token-2");

            ResponseEntity<Map> responseEntity =
                    new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    any(HttpEntity.class),
                    eq(Map.class))
            ).thenReturn(responseEntity);

            String token = vkTokenService.exchangeCodeForToken(code, codeVerifier, deviceId);
            assertEquals("vk-access-token-2", token);

            ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> captor =
                    ArgumentCaptor.forClass(HttpEntity.class);

            verify(restTemplate).postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    captor.capture(),
                    eq(Map.class)
            );

            MultiValueMap<String, String> params = captor.getValue().getBody();
            assertNotNull(params);
            assertNull(params.getFirst("device_id"));
        }

        @Test
        @DisplayName("Успешный обмен кода на токен без device_id (deviceId = пустая строка)")
        void exchangeCodeForToken_Success_WithoutDeviceId_Blank() {
            String code = "auth-code";
            String codeVerifier = "code-verifier";
            String deviceId = "   "; // blank

            Map<String, Object> body = new HashMap<>();
            body.put("access_token", "vk-access-token-3");

            ResponseEntity<Map> responseEntity =
                    new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    any(HttpEntity.class),
                    eq(Map.class))
            ).thenReturn(responseEntity);

            String token = vkTokenService.exchangeCodeForToken(code, codeVerifier, deviceId);
            assertEquals("vk-access-token-3", token);

            ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> captor =
                    ArgumentCaptor.forClass(HttpEntity.class);

            verify(restTemplate).postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    captor.capture(),
                    eq(Map.class)
            );

            MultiValueMap<String, String> params = captor.getValue().getBody();
            assertNotNull(params);
            assertNull(params.getFirst("device_id"));
        }

        @Test
        @DisplayName("Если VK вернул null body — выбрасывается RuntimeException")
        void exchangeCodeForToken_NullBody_Throws() {
            String code = "auth-code";
            String codeVerifier = "code-verifier";

            ResponseEntity<Map> responseEntity =
                    new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    any(HttpEntity.class),
                    eq(Map.class))
            ).thenReturn(responseEntity);

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> vkTokenService.exchangeCodeForToken(code, codeVerifier, null)
            );

            assertTrue(ex.getMessage().contains("VK вернул пустой ответ"));
        }

        @Test
        @DisplayName("Если VK не вернул access_token — выбрасывается RuntimeException с телом ответа")
        void exchangeCodeForToken_NoAccessToken_Throws() {
            String code = "auth-code";
            String codeVerifier = "code-verifier";

            Map<String, Object> body = new HashMap<>();
            body.put("some_other_field", "value");

            ResponseEntity<Map> responseEntity =
                    new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    any(HttpEntity.class),
                    eq(Map.class))
            ).thenReturn(responseEntity);

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> vkTokenService.exchangeCodeForToken(code, codeVerifier, null)
            );

            assertTrue(ex.getMessage().contains("VK не вернул access_token"));
            assertTrue(ex.getMessage().contains("some_other_field"));
        }

        @Test
        @DisplayName("Если RestTemplate бросает исключение — оборачивается в RuntimeException с сообщением")
        void exchangeCodeForToken_RestTemplateThrows_ThrowsRuntimeException() {
            String code = "auth-code";
            String codeVerifier = "code-verifier";

            when(restTemplate.postForEntity(
                    eq("https://id.vk.ru/oauth2/auth"),
                    any(HttpEntity.class),
                    eq(Map.class))
            ).thenThrow(new RuntimeException("HTTP 400"));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> vkTokenService.exchangeCodeForToken(code, codeVerifier, null)
            );

            assertTrue(ex.getMessage().contains("Не удалось получить access_token от VK"));
            assertTrue(ex.getMessage().contains("HTTP 400"));
        }
    }

    // --------- ТЕСТЫ ДЛЯ buildFrontendRedirectUrl ---------

    @Nested
    @DisplayName("buildFrontendRedirectUrl")
    class BuildFrontendRedirectUrlTests {

        @Test
        @DisplayName("Формирование URL с code, state и device_id")
        void buildFrontendRedirectUrl_WithDeviceId() {
            String code = "code+value";
            String state = "state value";
            String deviceId = "dev/id 123";

            String url = vkTokenService.buildFrontendRedirectUrl(code, state, deviceId);

            String expected = "https://frontend.example.com/callback" +
                    "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                    "&device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);

            assertEquals(expected, url);
        }

        @Test
        @DisplayName("Формирование URL без device_id (deviceId = null)")
        void buildFrontendRedirectUrl_WithoutDeviceId_Null() {
            String code = "code";
            String state = "state";
            String deviceId = null;

            String url = vkTokenService.buildFrontendRedirectUrl(code, state, deviceId);

            String expected = "https://frontend.example.com/callback" +
                    "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            assertEquals(expected, url);
            assertFalse(url.contains("device_id="));
        }

        @Test
        @DisplayName("Формирование URL без device_id (deviceId = пустая строка)")
        void buildFrontendRedirectUrl_WithoutDeviceId_Blank() {
            String code = "code";
            String state = "state";
            String deviceId = "   ";

            String url = vkTokenService.buildFrontendRedirectUrl(code, state, deviceId);

            String expected = "https://frontend.example.com/callback" +
                    "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            assertEquals(expected, url);
            assertFalse(url.contains("device_id="));
        }

        @Test
        @DisplayName("Значения code и state корректно URL-кодируются")
        void buildFrontendRedirectUrl_UrlEncoding() {
            String code = "cödé?&=";
            String state = "статус 1&2";

            String url = vkTokenService.buildFrontendRedirectUrl(code, state, null);

            String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);

            assertTrue(url.contains("code=" + encodedCode));
            assertTrue(url.contains("state=" + encodedState));
        }
    }
}