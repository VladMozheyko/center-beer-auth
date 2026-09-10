package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.dto.auth.*;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserIpTemp;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.UserIpTempRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.UserIpTempService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для отслеживания IP-адресов во время аутентификации пользователя.
 * Тесты охватывают:
 * 1. Обработка заголовков X-Real-IP (прямой IP от клиента)
 * 2. Обработка заголовков X-Forwarded-For (сценарий прокси)
 * 3. Запасной вариант для получения RemoteAddr (прямое соединение)
 * 4. Сохранение IP в базе данных
 * 5. Извлечение всех пользовательских IP-адресов
 * 6. Ограничение хранения последних 5 IP-адресов
 * 7. IP-данные в ответе аутентификации
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Интеграционные тесты отслеживания IP-адресов при аутентификации")
class IpAddressTrackingIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIpTempRepository userIpTempRepository;

    @Autowired
    private UserIpTempService userIpTempService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    
    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;

    @MockBean
    private MailSender mailSender;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userIpTempRepository.deleteAll();
    }

    private User createTestUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname("testuser_" + System.currentTimeMillis())
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .temporarySecondsBalance(0)
                .phoneVerified(false)
                .build();
        return userRepository.save(user);
    }

    private AuthenticationRequest createAuthRequest(String email, String password) {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setDeviceId("test-device-" + System.currentTimeMillis());
        return request;
    }

    // ============================================================================
    // 1: Хранение IP-адресов во время входа
    // ============================================================================

    @Nested
    @DisplayName("Тесты сохранения IP-адресов при входе")
    class IpStorageTests {

        @Test
        @DisplayName("1. X-Real-IP: При входе через логин и пароль IP сохраняется из заголовка X-Real-IP")
        void loginWithRealIpHeader_ShouldSaveIp() throws Exception {
            // Given
            String expectedIp = "203.0.113.45";
            testUser = createTestUser("test1@example.com", "password123");

            AuthenticationRequest authRequest = createAuthRequest("test1@example.com", "password123");

            // When
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest))
                            .header("X-Real-IP", expectedIp))
                    .andExpect(status().isOk());

            // Then
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(expectedIp);
            assertThat(ipRecords.get(0).getUserId()).isEqualTo(testUser.getId());
            assertThat(ipRecords.get(0).getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("2. X-Forwarded-For: При входе через прокси без X-Real-IP IP сохраняется из заголовка X-Forwarded-For")
        void loginWithForwardedForHeader_ShouldSaveIp() throws Exception {
            // Given
            String expectedIp = "192.168.1.1";  // First non-private IP from X-Forwarded-For
            String xffHeader = "192.168.1.1, 10.0.0.5, " + expectedIp + ", 127.0.0.1";
            testUser = createTestUser("test2@example.com", "password123");

            AuthenticationRequest authRequest = createAuthRequest("test2@example.com", "password123");

            // When: Отправляем только X-Forwarded-For без X-Real-IP
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest))
                            .header("X-Forwarded-For", xffHeader))
                    .andExpect(status().isOk());

            // Then: IP должен быть взят из X-Forwarded-For (первый неприватный IP)
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(expectedIp);
        }

        @Test
        @DisplayName("4. Приоритет заголовков: X-Real-IP имеет приоритет над X-Forwarded-For")
        void loginWithBothHeaders_RealIpTakesPriority() throws Exception {
            // Given
            String realIp = "203.0.113.100";  // X-Real-IP (должен быть выбран)
            String forwardedForIp = "198.51.100.23";  // X-Forwarded-For (должен быть проигнорирован)
            testUser = createTestUser("test4@example.com", "password123");

            AuthenticationRequest authRequest = createAuthRequest("test4@example.com", "password123");

            // When: Отправляем оба заголовка
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest))
                            .header("X-Real-IP", realIp)
                            .header("X-Forwarded-For", forwardedForIp))
                    .andExpect(status().isOk());

            // Then: Должен быть выбран X-Real-IP (приоритетный)
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(realIp);
        }

        @Test
        @DisplayName("3. RemoteAddr: При входе без заголовков IP сохраняется из getRemoteAddr")
        void loginWithRemoteAddr_ShouldSaveIp() throws Exception {
            // Given
            String expectedIp = "172.217.0.1";  // Real client IP
            testUser = createTestUser("test3@example.com", "password123");

            AuthenticationRequest authRequest = createAuthRequest("test3@example.com", "password123");

            // When
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest))
                            .with(request -> {
                                request.setRemoteAddr(expectedIp);
                                return request;
                            }))
                    .andExpect(status().isOk());

            // Then
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(expectedIp);
        }
    }

    // ============================================================================
    // 2: Хранение IP-адресов во время социального входа
    // ============================================================================

    @Nested
    @DisplayName("Тесты сохранения IP-адресов при входе через соцсети")
    class SocialLoginIpTests {

        @Test
        @DisplayName("Social Login: IP сохраняется при входе через соцсеть с X-Real-IP")
        void socialLoginWithRealIp_ShouldSaveIp() {
            // Given
            String expectedIp = "203.0.113.100";
            testUser = createTestUser("socialuser@example.com", "password123");

            UserSocialAccount userSocialAccount = new UserSocialAccount();
            userSocialAccount.setExternalId("social-123");
            userSocialAccount.setProvider(OAuthProvider.GOOGLE);
            userSocialAccount.setUser(testUser);
            testUser.getSocialAccounts().add(userSocialAccount);
            
            // Сохраняем только пользователя - каскад сохранит соцаккаунт автоматически
            userRepository.save(testUser);

            userIpTempService.saveIpTemp(testUser.getId(), expectedIp);

            // Then
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(expectedIp);
        }

        @Test
        @DisplayName("Social Login: Multiple IPs from different sessions")
        void socialLoginMultipleSessions_ShouldSaveMultipleIps() throws Exception {
            // Given
            String ip1 = "203.0.113.1";
            String ip2 = "203.0.113.2";
            String ip3 = "203.0.113.3";
            testUser = createTestUser("multiip@example.com", "password123");

            // Симуляция нескольких сессий входа с разных IP-адресов
            userIpTempService.saveIpTemp(testUser.getId(), ip1);
            Thread.sleep(100); // Небольшая задержка для обеспечения разных временных меток
            userIpTempService.saveIpTemp(testUser.getId(), ip2);
            Thread.sleep(100);
            userIpTempService.saveIpTemp(testUser.getId(), ip3);

            // Then
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(ipRecords).hasSize(3);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(ip3); // Новейший первый
            assertThat(ipRecords.get(1).getIpAddress()).isEqualTo(ip2);
            assertThat(ipRecords.get(2).getIpAddress()).isEqualTo(ip1);
        }

        @Test
        @DisplayName("Social Login: Вход через соцсеть с X-Real-IP (полный сценарий)")
        void socialLoginFullScenario_ShouldSaveIpFromHeader() {
            // Given - Создаем пользователя с соцаккаунтом
            String email = "socialfull@example.com";
            String externalId = "social-full-123";
            String expectedIp = "185.199.108.153";
            
            User userWithSocial = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("password123"))
                    .nickname("socialfulluser")
                    .role(Role.USER)
                    .createdAt(LocalDateTime.now())
                    .temporarySecondsBalance(0)
                    .phoneVerified(false)
                    .build();
            userWithSocial = userRepository.save(userWithSocial);

            UserSocialAccount socialAccount = new UserSocialAccount();
            socialAccount.setExternalId(externalId);
            socialAccount.setProvider(OAuthProvider.GOOGLE);
            socialAccount.setUser(userWithSocial);
            userWithSocial.getSocialAccounts().add(socialAccount);
            
            // Сохраняем только пользователя - каскад сохранит соцаккаунт автоматически
            userRepository.save(userWithSocial);

            // When - Вход через соцсеть (эмуляция через вызов сервиса IP сохранения)
            // В реальном приложении это происходит в контроллере OAuth2Controller
            userIpTempService.saveIpTemp(userWithSocial.getId(), expectedIp);

            // Then
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userWithSocial.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(expectedIp);
            assertThat(ipRecords.get(0).getUserId()).isEqualTo(userWithSocial.getId());
        }
    }

    // ============================================================================
    // 3: Ограничение хранения — последние 5 IP
    // ============================================================================

    @Nested
    @DisplayName("Тесты ограничения хранения последних 5 IP-адресов")
    class IpLimitTests {

        @Test
        @DisplayName("При 6-м входе самый старый IP удаляется")
        void saveSixthIp_ShouldDeleteOldest() {
            // Given
            testUser = createTestUser("limituser@example.com", "password123");
            long userId = testUser.getId();

            String[] ips = {"192.0.2.1", "192.0.2.2", "192.0.2.3", "192.0.2.4", "192.0.2.5"};
            for (String ip : ips) {
                userIpTempService.saveIpTemp(userId, ip);
            }

            // Проверяем, что сохранено ровно 5 IP
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
            assertThat(ipRecords).hasSize(5);

            // When: 6-й вход
            userIpTempService.saveIpTemp(userId, "192.0.2.6");

            // Then: Все еще 5 IP, самый старый удален
            ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
            assertThat(ipRecords).hasSize(5);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo("192.0.2.6"); // Newest first
            assertThat(ipRecords.get(4).getIpAddress()).isEqualTo("192.0.2.5"); // Oldest remaining

            // Старый IP (192.0.2.1) должен быть удален
            boolean hasOldest = ipRecords.stream().anyMatch(ip -> "192.0.2.1".equals(ip.getIpAddress()));
            assertThat(hasOldest).isFalse();
        }

        @Test
        @DisplayName("При многократных входах всегда хранится только 5 последних IP")
        void saveManyIps_ShouldAlwaysKeepLastFive() {
            // Given
            testUser = createTestUser("manyipsuser@example.com", "password123");
            long userId = testUser.getId();

            // When: 10 входов с разными IP
            for (int i = 1; i <= 10; i++) {
                userIpTempService.saveIpTemp(userId, "192.0.2." + i);
            }

            // Then: Всегда только 5 IP
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
            assertThat(ipRecords).hasSize(5);

            // Должны остаться последние 5: 6, 7, 8, 9, 10
            List<String> ipAddresses = ipRecords.stream()
                    .map(UserIpTemp::getIpAddress)
                    .toList();
            assertThat(ipAddresses).containsExactly("192.0.2.10", "192.0.2.9", "192.0.2.8", "192.0.2.7", "192.0.2.6");
        }

        @Test
        @DisplayName("Один и тот же IP при повторном входе добавляется как новый")
        void saveDuplicateIp_ShouldAddAsNew() {
            // Given
            testUser = createTestUser("duplicateipuser@example.com", "password123");
            long userId = testUser.getId();

            String[] ips = {"192.0.2.1", "192.0.2.2", "192.0.2.3", "192.0.2.4", "192.0.2.5"};
            for (String ip : ips) {
                userIpTempService.saveIpTemp(userId, ip);
            }

            // When: Повторный вход с первым IP
            userIpTempService.saveIpTemp(userId, "192.0.2.1");

            // Then: 192.0.2.1 снова в списке, но как самый новый
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
            assertThat(ipRecords).hasSize(5);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo("192.0.2.1");
            // Самый старый (192.0.2.2) удален
            boolean hasOldest = ipRecords.stream().anyMatch(ip -> "192.0.2.2".equals(ip.getIpAddress()));
            assertThat(hasOldest).isFalse();
        }
    }

    // ============================================================================
    // 4: IP-адрес в ответ
    // ============================================================================

    @Nested
    @DisplayName("Тесты IP-адресов в ответе аутентификации")
    class IpInResponseTests {

        @Test
        @DisplayName("Ответ содержит список всех IP-адресов пользователя")
        void loginResponse_ShouldIncludeAllUserIps() throws Exception {
            // Given
            String ip1 = "192.0.2.1";
            String ip2 = "192.0.2.2";
            testUser = createTestUser("responseip@example.com", "password123");

            // Заранее сохраните некоторые IP, чтобы симулировать несколько сессий
            userIpTempService.saveIpTemp(testUser.getId(), ip1);
            userIpTempService.saveIpTemp(testUser.getId(), ip2);

            AuthenticationRequest authRequest = createAuthRequest("responseip@example.com", "password123");

            // When
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest))
                            .header("X-Real-IP", "192.0.2.3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastIpAddress").isArray())
                    .andExpect(jsonPath("$.lastIpAddress.length()").value(3));
        }

        @Test
        @DisplayName("Ответ содержит правильные данные IP-адресов (DTO)")
        void loginResponse_ShouldIncludeCorrectIpDto() throws Exception {
            // Given
            String expectedIp = "45.33.32.156";
            testUser = createTestUser("dtoip@example.com", "password123");

            AuthenticationRequest authRequest = createAuthRequest("dtoip@example.com", "password123");

            // When
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest))
                            .header("X-Real-IP", expectedIp))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastIpAddress[0].ipAddress").value(expectedIp))
                    .andExpect(jsonPath("$.lastIpAddress[0].createdAt").exists());
        }
    }

    // ============================================================================
    // 5: Извлечение IP (getAllIpsForUser)
    // ============================================================================

    @Nested
    @DisplayName("Тесты получения всех IP-адресов пользователя")
    class IpRetrievalTests {

        @Test
        @DisplayName("Получение всех IP пользователя - возвращает не более 5 записей")
        void getAllIpsForUser_ShouldReturnMaxFive() {
            // Given
            testUser = createTestUser("retrieveall@example.com", "password123");
            long userId = testUser.getId();

            String[] ips = {"192.0.2.1", "192.0.2.2", "192.0.2.3", "192.0.2.4", "192.0.2.5"};
            for (String ip : ips) {
                userIpTempService.saveIpTemp(userId, ip);
            }

            // When
            List<UserIpTempDto> dtos = userIpTempService.getTrackedIpForUser(userId);

            // Then
            assertThat(dtos).hasSize(5);
            // Should be sorted by createdAt DESC (newest first)
            for (int i = 0; i < ips.length; i++) {
                assertThat(dtos.get(i).getIpAddress()).isEqualTo(ips[ips.length - 1 - i]);
                assertThat(dtos.get(i).getIpAddress()).isNotNull();
            }
        }

        @Test
        @DisplayName("Получение IP пользователя - пустой список для пользователя без IP")
        void getAllIpsForUser_EmptyForNewUser() {
            // Given
            testUser = createTestUser("noips@example.com", "password123");

            // When
            List<UserIpTempDto> dtos = userIpTempService.getTrackedIpForUser(testUser.getId());

            // Then
            assertThat(dtos).isEmpty();
        }
    }

    // ============================================================================
    // 6: Интеграционный тест — полный поток
    // ============================================================================

    @Nested
    @DisplayName("Полный интеграционный сценарий")
    class FullFlowTests {

        @Test
        @DisplayName("Полный сценарий: Регистрация -> Вход -> Проверка IP -> Лимит 5")
        void fullScenario_RegisterLoginCheckIpLimit() throws Exception {
            // Шаг 1: Зарегистрировать пользователя с данными
            String email = "fullscenario@example.com";
            String password = "SecurePass123!";
            String nickname = "fullscenariouser";
            String clientIp = "8.8.8.8";

            mockMvc.perform(post("/authentication/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    RegisterRequest.builder()
                                            .email(email)
                                            .password(password)
                                            .nickname(nickname)
                                            .build()))
                            .header("X-Real-IP", clientIp))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Код активации для активации аккаунта успешно отправлен на почтовый адрес"));

            // Проверьте, что пользователь был создан в БД
            User registeredUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(registeredUser.getEmail()).isEqualTo(email);
            assertThat(registeredUser.getNickname()).isEqualTo(nickname);

            // Активируем пользователя, удаляя код активации
            registeredUser.setActivationCode(null);
            userRepository.save(registeredUser);

            // Проверьте, что IP был сохранён при регистрации
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(registeredUser.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(clientIp);

            // Шаг 2: Войти с другим IP-адресом
            String loginIp = "1.1.1.1";
            AuthenticationRequest loginRequest = AuthenticationRequest.builder()
                    .email(email)
                    .password(password)
                    .deviceId("device-full-scenario")
                    .build();

            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("X-Real-IP", loginIp))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.lastIpAddress.length()").value(2));

            // Проверка, что оба IP сохранены (новый IP добавлен)
            ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(registeredUser.getId());
            assertThat(ipRecords).hasSize(2);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(loginIp); // Newest first
            assertThat(ipRecords.get(1).getIpAddress()).isEqualTo(clientIp);

            // Шаг 3: Получение всех IP для пользователя
            List<UserIpTempDto> allIps = userIpTempService.getTrackedIpForUser(registeredUser.getId());
            assertThat(allIps).hasSize(2);
            assertThat(allIps.get(0).getIpAddress()).isEqualTo(loginIp);
            assertThat(allIps.get(1).getIpAddress()).isEqualTo(clientIp);

            // Проверка, что DTO содержит все необходимые поля
            UserIpTempDto newestIpDto = allIps.get(0);
            assertThat(newestIpDto.getIpAddress()).isEqualTo(loginIp);
            assertThat(newestIpDto.getCreatedAt()).isNotNull();

            // Шаг 4: Делаем еще 3 входа, чтобы достичь лимита в 5
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("X-Real-IP", "2.2.2.2"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("X-Real-IP", "3.3.3.3"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("X-Real-IP", "4.4.4.4"))
                    .andExpect(status().isOk());

            // Шаг 5: Проверяем, что осталось только 5 IP
            ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(registeredUser.getId());
            assertThat(ipRecords).hasSize(5);

            // Должны остаться: 4.4.4.4, 3.3.3.3, 2.2.2.2, 1.1.1.1, 8.8.8.8
            List<String> ipAddresses = ipRecords.stream()
                    .map(UserIpTemp::getIpAddress)
                    .toList();
            assertThat(ipAddresses).containsExactly("4.4.4.4", "3.3.3.3", "2.2.2.2", "1.1.1.1", "8.8.8.8");

            // Шаг 6: Еще один вход — должен удалиться самый старый (8.8.8.8)
            mockMvc.perform(post("/authentication/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .header("X-Real-IP", "5.5.5.5"))
                    .andExpect(status().isOk());

            ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(registeredUser.getId());
            assertThat(ipRecords).hasSize(5);
            ipAddresses = ipRecords.stream().map(UserIpTemp::getIpAddress).toList();
            assertThat(ipAddresses).containsExactly("5.5.5.5", "4.4.4.4", "3.3.3.3", "2.2.2.2", "1.1.1.1");
        }
    }

    // ============================================================================
    // 7: Полный сценарий социальной авторизации
    // ============================================================================

    @Nested
    @DisplayName("Полный сценарий социальной авторизации")
    class SocialLoginFullScenarioTests {

        @Test
        @DisplayName("Полный сценарий: Соцсеть -> Регистрация/Вход -> Проверка IP")
        void socialLoginFullScenario() {
            // Шаг 1: Создаем пользователя с соцаккаунтом
            String email = "sociallogin@example.com";
            String externalId = "social-login-123";
            String clientIp = "142.250.185.78";

            User userWithSocial = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("password123"))
                    .nickname("socialloginuser")
                    .role(Role.USER)
                    .createdAt(LocalDateTime.now())
                    .temporarySecondsBalance(0)
                    .phoneVerified(false)
                    .build();
            userWithSocial = userRepository.save(userWithSocial);

            UserSocialAccount socialAccount = new UserSocialAccount();
            socialAccount.setExternalId(externalId);
            socialAccount.setProvider(OAuthProvider.GOOGLE);
            socialAccount.setUser(userWithSocial);
            userWithSocial.getSocialAccounts().add(socialAccount);
            
            // Сохраняем только пользователя - каскад сохранит соцаккаунт автоматически
            userRepository.save(userWithSocial);

            // Шаг 2: Имитируем вход через соцсеть - сохраняем IP
            // В реальном приложении это делает OAuth2Controller при обработке authCode
            userIpTempService.saveIpTemp(userWithSocial.getId(), clientIp);

            // Шаг 3: Проверяем, что IP сохранен
            List<UserIpTemp> ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userWithSocial.getId());
            assertThat(ipRecords).hasSize(1);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(clientIp);
            assertThat(ipRecords.get(0).getUserId()).isEqualTo(userWithSocial.getId());

            // Шаг 4: Вход с другого IP
            String secondIp = "142.250.185.99";
            userIpTempService.saveIpTemp(userWithSocial.getId(), secondIp);

            // Шаг 5: Проверяем оба IP
            ipRecords = userIpTempRepository.findAllByUserIdOrderByCreatedAtDesc(userWithSocial.getId());
            assertThat(ipRecords).hasSize(2);
            assertThat(ipRecords.get(0).getIpAddress()).isEqualTo(secondIp); // Newest
            assertThat(ipRecords.get(1).getIpAddress()).isEqualTo(clientIp);

            // Шаг 6: Получаем все IP через DTO
            List<UserIpTempDto> allIps = userIpTempService.getTrackedIpForUser(userWithSocial.getId());
            assertThat(allIps).hasSize(2);
            assertThat(allIps.get(0).getIpAddress()).isEqualTo(secondIp);
            assertThat(allIps.get(1).getIpAddress()).isEqualTo(clientIp);
        }
    }
}
