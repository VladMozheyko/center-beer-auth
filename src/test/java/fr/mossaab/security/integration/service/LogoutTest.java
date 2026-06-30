package fr.mossaab.security.integration.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponseDto;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест сценария выхода
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LogoutTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    Random rnd = new Random();

    @Value("${application.security.jwt.refresh-token.cookie-name}")
    private String refreshTokenName;

    @Test
    public void testLogoutScenario() throws Exception {
        // Шаг 1: Вход с 4 разных устройств
        System.out.println("Step 1: Вход с четырёх разных устройств");
        AuthenticationRequest loginRequest = createAuthenticationRequest("Vlad72229@yandex.ru", "Vlad!123", "");
        AuthenticationResponseDto firstLogin = requestOld(loginRequest); // Первая сессия
        AuthenticationResponseDto secondLogin = requestOld(loginRequest); // Вторая сессия
        AuthenticationResponseDto thirdLogin = requestOld(loginRequest); // Третья сессия
        AuthenticationResponseDto fourthLogin = requestOld(loginRequest); // Четвёртая сессия

        checkTokens(1L, 3); // Ожидаем что 3 активных и 1 отозванный

        // Шаг 2: Выход со всех устройств кроме текущего
        System.out.println("Step 2: Выход из всех устройств, кроме текущего,");
        requestLogoutAll(fourthLogin.getRefreshToken(), false);
        checkTokens(1L, 1); // Ожидаем что остался 1 токен
        RefreshToken rt = refreshTokenRepository.findAll().get(0);
        assertThat(rt.getToken()).isEqualTo(fourthLogin.getRefreshToken());


        // Шаг 3: Вход с трёх устройств
        System.out.println("Step 3: Снова вход с трёх устройств");
        requestOld(loginRequest); // Пятая сессия
        requestOld(loginRequest); // Шестая сессия
        requestOld(loginRequest); // Седьмая сессия

        checkTokens(1L, 3); // Базе снова 4 токена, три активных

        // Шаг 4: Выход со всех устройств и с текущего
        System.out.println("Step 4: Выход из системы со всех устройств, включая текущий");
        requestLogoutAll(fourthLogin.getRefreshToken(), true);
        checkTokens(1L, 0); // Должно быть 0 токенов

        System.out.println("Все этапы выполнены успешно — тест на выход из выхода пройден!");
    }

    private AuthenticationRequest createAuthenticationRequest(String email, String password, String deviceId) {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setDeviceId(deviceId);
        return request;
    }

    private void checkTokens(Long userId, int expectedCountRefreshTokens) {
        Instant now = Instant.now();
        var listTokens = refreshTokenRepository.findByUserIdAndExpiryDateAfter(userId, now);
        var revokedFalse = listTokens.stream().filter(rt -> !rt.revoked).toList();
        assertThat(revokedFalse).hasSize(expectedCountRefreshTokens);
    }

    private AuthenticationResponseDto requestOld(AuthenticationRequest request) throws Exception {
        List<String> device = new ArrayList<>(List.of("Android" + rnd.nextInt(4, 17), "Windows" + rnd.nextInt(7, 12)));
        String requestJson = mapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", device.get(rnd.nextInt(device.size())))
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").exists())
                .andDo(print())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return mapper.readValue(responseJson, AuthenticationResponseDto.class);
    }

    private void requestLogoutAll(String refreshToken, boolean loggingOutOfThisDevice) throws Exception {
        mockMvc.perform(post("/auth-session/logout/all")
                        .cookie(new Cookie(refreshTokenName, refreshToken))
                        .with(user("Vlad72229@yandex.ru"))
                        .param("loggingOutOfThisDevice", String.valueOf(loggingOutOfThisDevice)))
                .andExpect(status().isOk());
    }
}
