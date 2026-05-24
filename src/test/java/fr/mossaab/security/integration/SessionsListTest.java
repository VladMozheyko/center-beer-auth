package fr.mossaab.security.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.mossaab.security.dto.SessionInfoResponse;
import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponseDto;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест сценария получения списка активных сессий (/auth-session/sessions)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SessionsListTest {

    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final Random rnd = new Random();

    @BeforeAll
    public static void setUp() {
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testSessionsScenario() throws Exception {
        Long userId = 1L; // тот же пользователь, которого используешь в LogoutTest

        // Шаг 1: первый логин — 1 сессия
        AuthenticationRequest loginRequest = createAuthenticationRequest(
                "Vlad72229@yandex.ru",
                "Vlad!123",
                ""
        );

        System.out.println("Step 1: Первый вход — одна активная сессия");
        AuthenticationResponseDto firstLogin = doLogin(loginRequest);
        assertTokensInDb(userId, 1, 0);      // 1 неотозванный


        // Шаг 2: второй логин — 2 сессии
        System.out.println("Step 2: Второй вход — две активных сессии");
        AuthenticationResponseDto secondLogin = doLogin(loginRequest);
        assertTokensInDb(userId, 2, 0);

        // Шаг 3: третий логин — 3 сессии
        System.out.println("Step 3: Третий вход — три активных сессии");
        AuthenticationResponseDto thirdLogin = doLogin(loginRequest);
        assertTokensInDb(userId, 3, 0);

        // Шаг 4: четвертый логин — должно стать 4 записи в БД,
        // но одна из них отозвана (по лимиту сессий)
        System.out.println("Step 4: Четвертый вход — всего 4 сессии, одна отозвана");
        AuthenticationResponseDto fourthLogin = doLogin(loginRequest);

        // Проверка в БД
        assertTokensInDb(userId, 3, 1); // 3 активные, 1 отозвана

        // Проверка через /auth-session/sessions
        assertSessionsEndpoint(4, 1);

        System.out.println("Тест сценария списка сессий успешно пройден!");
    }

    // ---------- Вспомогательные методы ----------

    private AuthenticationRequest createAuthenticationRequest(String email, String password, String deviceId) {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setDeviceId(deviceId);
        return request;
    }

    /**
     * Логин с рандомным User-Agent, как в LogoutTest.
     */
    private AuthenticationResponseDto doLogin(AuthenticationRequest request) throws Exception {
        List<String> device = new ArrayList<>(List.of(
                "Android" + rnd.nextInt(4, 17),
                "Windows" + rnd.nextInt(7, 12)
        ));
        String requestJson = mapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", device.get(rnd.nextInt(device.size())))
                        .content(requestJson))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return mapper.readValue(responseJson, AuthenticationResponseDto.class);
    }

    /**
     * Проверка состояния токенов в БД.
     *
     * @param userId                ID пользователя
     * @param expectedActive        ожидаемое количество неотозванных токенов
     * @param expectedRevoked       ожидаемое количество отозванных токенов
     */
    private void assertTokensInDb(Long userId, int expectedActive, int expectedRevoked) {
        Instant now = Instant.now();
        List<RefreshToken> listTokens = refreshTokenRepository.findByUserIdAndExpiryDateAfter(userId, now);

        long active = listTokens.stream().filter(rt -> !rt.isRevoked()).count();
        long revoked = listTokens.stream().filter(RefreshToken::isRevoked).count();

        assertThat(active).isEqualTo(expectedActive);
        assertThat(revoked).isEqualTo(expectedRevoked);
        assertThat(listTokens.size()).isEqualTo(expectedActive + expectedRevoked);
    }

    /**
     * Запрос к /auth-session/sessions и проверка, что
     * верное количество сессий и корректное число revoked=true.
     *
     * @param expectedTotalSessions  всего сессий в ответе
     * @param expectedRevokedCount   сколько из них помечены revoked=true
     */
    private void assertSessionsEndpoint(int expectedTotalSessions, int expectedRevokedCount) throws Exception {
        MvcResult result = mockMvc.perform(get("/auth-session/sessions")
                        .with(user("Vlad72229@yandex.ru")))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        // Используем TypeReference для десериализации списка
        List<SessionInfoResponse> sessions = mapper.readValue(json, new TypeReference<List<SessionInfoResponse>>() {});


        assertThat(sessions).hasSize(expectedTotalSessions);

        long revokedCount = sessions.stream()
                .filter(SessionInfoResponse::isRevoked)
                .count();

        assertThat(revokedCount).isEqualTo(expectedRevokedCount);
    }
}
