package fr.mossaab.security.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponseDto;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тест сценария аутентификации с управлением deviceId и refresh‑токенами.
 * <p>Проверяет поведение системы при:
 * 1. Первичном входе без указания deviceId (система генерирует новый).
 * 2. Повторном входе без deviceId (создаётся новый deviceId).
 * 3. Входе с ранее полученным deviceId (используется существующий).
 * 4. Последующих входах без deviceId (проверяется ограничение на количество refresh‑токенов).
 * </p>
 * Ожидаемое поведение:
 * - При отсутствии deviceId в запросе система генерирует новый.
 * - При указании существующего deviceId используется он.
 * - Количество refresh‑токенов для пользователя ограничено (не превышает заданный лимит 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthenticationDeviceManagementTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    Random rnd = new Random(3);

    /**
     * Тест проверяет сценарий управления deviceId при последовательных входах:
     * 1. Первый вход без deviceId → генерируется новый deviceId.
     * 2. Второй вход без deviceId → создаётся новый deviceId, проверяется количество refresh‑токенов.
     * 3. Вход с ранее полученным deviceId → используется существующий deviceId.
     * 4. Последующие входы без deviceId → проверяется ограничение на количество refresh‑токенов.
     *
     * @throws Exception если возникает ошибка при выполнении HTTP‑запроса
     */
    @Test
    public void testDeviceIdManagementWithSequentialLogins() throws Exception {
        // Шаг 1: Первый вход без указания deviceId
        System.out.println("Step 1: First login without deviceId");
        AuthenticationRequest loginRequest = createAuthenticationRequest("Vlad72229@yandex.ru", "Vlad!123", "");
        AuthenticationResponseDto firstLogin = request(loginRequest);

        assertThat(firstLogin.getDeviceId()).isNotNull();
        assertThat(firstLogin.getDeviceId()).isNotBlank();
        System.out.println("Step 1 Success - Generated deviceId: " + firstLogin.getDeviceId());

        // Шаг 2: Второй вход без указания deviceId — ожидается новый deviceId
        System.out.println("Step 2: Second login without deviceId (new deviceId expected)");
        AuthenticationResponseDto secondLogin = request(loginRequest);
        assertThat(secondLogin.getDeviceId()).isNotNull();
        assertThat(secondLogin.getDeviceId()).isNotEqualTo(firstLogin.getDeviceId()); // Должен быть новый deviceId
        checkTokens(1L, 2); // Ожидаем 2 refresh‑токена
        System.out.println("Step 2 Success - New deviceId: " + secondLogin.getDeviceId());

        // Шаг 3: Вход с ранее полученным deviceId — используем первый deviceId
        System.out.println("Step 3: Login with previously obtained deviceId");
        loginRequest.setDeviceId(firstLogin.getDeviceId());
        AuthenticationResponseDto thirdLogin = request(loginRequest);
        assertThat(thirdLogin.getDeviceId()).isEqualTo(firstLogin.getDeviceId()); // Должен вернуться тот же deviceId
        checkTokens(1L, 2); // Количество токенов не должно увеличиться
        System.out.println("Step 3 Success - Reused deviceId: " + thirdLogin.getDeviceId());

        // Шаг 4: Вход без deviceId после использования существующего — ожидается новый deviceId
        System.out.println("Step 4: Login without deviceId after using existing one");
        loginRequest.setDeviceId("");
        AuthenticationResponseDto fourthLogin = request(loginRequest);
        assertThat(fourthLogin.getDeviceId()).isNotNull();
        assertThat(fourthLogin.getDeviceId())
                .isNotEqualTo(firstLogin.getDeviceId())
                .isNotEqualTo(secondLogin.getDeviceId());
        checkTokens(1L, 3); // Ожидаем 3 refresh‑токена (лимит достигнут)
        System.out.println("Step 4 Success - Another new deviceId: " + fourthLogin.getDeviceId());

        // Шаг 5: Ещё один вход без deviceId — проверяем, что лимит токенов не превышен
        System.out.println("Step 5: Final login without deviceId — token limit check");
        AuthenticationResponseDto fifthLogin = request(loginRequest);
        assertThat(fifthLogin.getDeviceId()).isNotNull();
        checkTokens(1L, 3); // Количество токенов должно остаться 3
        System.out.println("Step 5 Success - Token limit maintained: 3 tokens");

        // Шаг 6: Используем deviceId который уже отозван - должен создать новый но отозвать последний вход
        System.out.println("Step 6: ");
        loginRequest.setDeviceId(firstLogin.getDeviceId());
        AuthenticationResponseDto sixLogin = request(loginRequest);
        assertThat(sixLogin.getDeviceId()).isNotNull();
        assertThat(sixLogin.getDeviceId()).isNotEqualTo(fifthLogin.getDeviceId());
        checkTokens(1L, 3);
        System.out.println("Step 6 Success - Token limit maintained: 3 tokens");

        System.out.println("All steps completed successfully — Device ID management test passed!");
        
        // В итоге в базе должно быть 5 токенов, 2 из которых отозваны
        List<RefreshToken> byUserId = refreshTokenRepository.findByUserId(1L);
        assertThat(byUserId.size()).isEqualTo(5);
        List<RefreshToken> tokenRevoked = byUserId.stream().filter(t->t.revoked).toList();
        List<RefreshToken> tokenNotRevoked = byUserId.stream().filter(t-> !t.revoked).toList();
        assertThat(tokenRevoked.size()).isEqualTo(2);
        assertThat(tokenNotRevoked.size()).isEqualTo(3);

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
        var revokedFalse = listTokens.stream().filter(rt-> !rt.revoked).toList();
        assertThat(revokedFalse.size()).isEqualTo(expectedCountRefreshTokens);
    }

    private AuthenticationResponseDto request(AuthenticationRequest request) throws Exception {

        List<String> device = new ArrayList<>(List.of("Android" +  rnd.nextInt(4,17), "Windows" +  rnd.nextInt(7, 12)));
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
}
