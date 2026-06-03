package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.user.LocationDto;
import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.entities.Location;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.LocationRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.MailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для UserController.
 * - Реальные UserService, UserRepository, LocationRepository, база данных.
 * - MailSender замокан, его поведение не проверяем.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MailSender mailSender;

    private User existingUser;

    @BeforeEach
    void setUp() {
        // Чистим таблицы
        userRepository.deleteAll();

        // Создаём базового пользователя, который будет "текущим"
        existingUser = User.builder()
                .email("user@example.com")
                .nickname("oldNick")
                .temporarySecondsBalance(1000)
                .createdAt(LocalDateTime.now())
                .build();
        existingUser = userRepository.save(existingUser);
    }

    // ==========================
    // GET /user/profile
    // ==========================

    @Nested
    @DisplayName("GET /user/profile")
    class GetProfileTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("возвращает профиль текущего пользователя без локации")
        void getProfile_noLocation() throws Exception {
            mockMvc.perform(get("/user/profile"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(existingUser.getId()))
                    .andExpect(jsonPath("$.nickname").value("oldNick"))
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.country").doesNotExist())
                    .andExpect(jsonPath("$.city").doesNotExist())
                    .andExpect(jsonPath("$.latitude").doesNotExist())
                    .andExpect(jsonPath("$.longitude").doesNotExist());
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("возвращает профиль с локацией, если она есть")
        void getProfile_withLocation() throws Exception {
            Location location = Location.builder()
                    .country("Russia")
                    .city("Moscow")
                    .latitude(55.7558)
                    .longitude(37.6173)
                    .build();
            User user = userRepository.findByEmail(existingUser.getEmail()).orElseThrow(()->new RuntimeException("Ошибка при получении пользователя"));
            user.setLocation(location);
            userRepository.save(user);

            String responseJson = mockMvc.perform(get("/user/profile"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(existingUser.getId()))
                    .andExpect(jsonPath("$.country").value("Russia"))
                    .andExpect(jsonPath("$.city").value("Moscow"))
                    .andExpect(jsonPath("$.latitude").value(55.7558))
                    .andExpect(jsonPath("$.longitude").value(37.6173))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            UserProfileResponse profile = objectMapper.readValue(responseJson, UserProfileResponse.class);
            assertThat(profile.getId()).isEqualTo(existingUser.getId());
        }

        @Test
        @WithMockUser(username = "unknown@example.com")
        @DisplayName("если пользователь не найден по email → 404 NotFound")
        void getProfile_userNotFound() throws Exception {
            mockMvc.perform(get("/user/profile"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==========================
    // PATCH /user/update-nickname
    // ==========================

    @Nested
    @DisplayName("PATCH /user/update-nickname")
    class UpdateNicknameTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("успешно изменяет никнейм, если он свободен")
        void updateNickname_success() throws Exception {
            String newNickname = "newNickName";

            mockMvc.perform(patch("/user/update-nickname")
                            .param("newNickname", newNickname))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Никнейм успешно обновлён на " + newNickname));

            User updated = userRepository.findByEmail("user@example.com").orElseThrow();
            assertThat(updated.getNickname()).isEqualTo(newNickname);
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("если ник уже занят другим активным пользователем → DuplicateResourceException → 409 (если не перехватывается глобально)")
        void updateNickname_duplicateNickname() throws Exception {
            User another = User.builder()
                    .email("another@example.com")
                    .nickname("takenNick")
                    .createdAt(LocalDateTime.now())
                    .temporarySecondsBalance(2000)
                    .build();
            // activationCode == null → активный
            another.setActivationCode(null);
            userRepository.save(another);

            mockMvc.perform(patch("/user/update-nickname")
                            .param("newNickname", "takenNick"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("если новый ник не проходит валидацию по аннотации → 400 BadRequest")
        void updateNickname_validationError() throws Exception {
            // предполагаем, что аннотация @ValidRuEnNicknameLengthMin4Max50
            // не пропустит строку длиной 2
            mockMvc.perform(patch("/user/update-nickname")
                            .param("newNickname", "ab"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==========================
    // POST /user/request-email-change
    // ==========================

    @Nested
    @DisplayName("POST /user/request-email-change")
    class RequestEmailChangeTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("успешный запрос на смену email → 200 и activationCode/tempEmail сохранены")
        void requestEmailChange_success() throws Exception {
            String newEmail = "new-email@example.com";

            doNothing().when(mailSender).send(anyString(), anyString(), anyString());

            mockMvc.perform(post("/user/request-email-change")
                            .param("newEmail", newEmail))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Ссылка для подтверждения отправлена на " + newEmail));

            User user = userRepository.findByEmail("user@example.com").orElseThrow();
            assertThat(user.getTempEmail()).isEqualTo(newEmail);
            assertThat(user.getActivationCode()).isNotNull();
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("валидация email (аннотация @ValidEmail) → 400 при некорректном адресе")
        void requestEmailChange_invalidEmail() throws Exception {
            mockMvc.perform(post("/user/request-email-change")
                            .param("newEmail", "not-an-email"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(mailSender);
        }

        @Test
        @WithMockUser(username = "unknown@example.com")
        @DisplayName("если пользователь не найден по email → 404")
        void requestEmailChange_userNotFound() throws Exception {
            mockMvc.perform(post("/user/request-email-change")
                            .param("newEmail", "valid@example.com"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==========================
    // GET /user/confirm-email-change
    // ==========================

    @Nested
    @DisplayName("GET /user/confirm-email-change")
    class ConfirmEmailChangeTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("успешное подтверждение смены email → 200 и email изменяется")
        void confirmEmailChange_success() throws Exception {
            String activationCode = "1234"; // ВАЖНО: 4 цифры, проходит @ValidSmsCode
            String newEmail = "confirmed@example.com";

            User user = userRepository.findByEmail(existingUser.getEmail()).orElseThrow();
            user.setActivationCode(activationCode);
            user.setTempEmail(newEmail);
            userRepository.saveAndFlush(user);

            mockMvc.perform(get("/user/confirm-email-change")
                            .param("code", activationCode))
                    .andExpect(status().isOk())
                    .andExpect(content().string("E-mail успешно изменён"));

            Optional<User> byOldEmail = userRepository.findByEmail("user@example.com");
            assertThat(byOldEmail).isEmpty();

            User updated = userRepository.findByEmail(newEmail).orElseThrow();
            assertThat(updated.getTempEmail()).isNull();
            assertThat(updated.getActivationCode()).isNull();
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("неверный код → 400 и сообщение из IllegalArgumentException")
        void confirmEmailChange_invalidCode() throws Exception {
            mockMvc.perform(get("/user/confirm-email-change")
                            .param("code", "unknown-code"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.message").value("Параметры запроса не прошли валидацию"))
                    .andExpect(jsonPath("$.errors").value(
                            containsString("Код должен состоять из 4 цифр")
                    ));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("отсутствует tempEmail → 400 и сообщение из IllegalStateException")
        void confirmEmailChange_noTempEmail() throws Exception {
            String activationCode = "1234"; // ВАЖНО: 4 цифры, проходит @ValidSmsCode

            User user = userRepository.findByEmail(existingUser.getEmail()).orElseThrow();
            user.setActivationCode(activationCode);
            user.setTempEmail(null);
            userRepository.saveAndFlush(user);

            mockMvc.perform(get("/user/confirm-email-change")
                            .param("code", activationCode))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Отсутствует новый e-mail для изменения"));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("новый email уже занят другим пользователем → 400 с сообщением DuplicateResourceException")
        void confirmEmailChange_emailAlreadyUsed() throws Exception {
            // другой пользователь с email, который станет tempEmail
            User other = User.builder()
                    .email("already-used@example.com")
                    .nickname("someNick")
                    .createdAt(LocalDateTime.now())
                    .temporarySecondsBalance(100)
                    .build();
            userRepository.save(other);

            // ВАЖНО: код должен соответствовать @ValidSmsCode -> 4 цифры
            String activationCode = "1234";

            User existingUserDownloaded = userRepository.findByEmail(existingUser.getEmail()).orElseThrow();
            existingUserDownloaded.setActivationCode(activationCode);
            existingUserDownloaded.setTempEmail("already-used@example.com");
            userRepository.saveAndFlush(existingUserDownloaded);

            mockMvc.perform(get("/user/confirm-email-change")
                            .param("code", activationCode))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Данный e-mail уже используется другим пользователем"));
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("валидация кода по @ValidSmsCode → 400 при неверном формате")
        void confirmEmailChange_invalidCodeFormat() throws Exception {
            // зависит от реализации @ValidSmsCode, предположим, что пустая строка невалидна
            mockMvc.perform(get("/user/confirm-email-change")
                            .param("code", ""))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==========================
    // DELETE /user/delete
    // ==========================

    @Nested
    @DisplayName("DELETE /user/delete")
    class DeleteAccountTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("успешное удаление аккаунта текущего пользователя")
        void deleteAccount_success() throws Exception {
            mockMvc.perform(delete("/user/delete"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Аккаунт успешно удалён"));

            assertThat(userRepository.findById(existingUser.getId())).isEmpty();
        }

        @Test
        @WithMockUser(username = "unknown@example.com")
        @DisplayName("если пользователь не найден по email → 404")
        void deleteAccount_userNotFound() throws Exception {
            mockMvc.perform(delete("/user/delete"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==========================
    // POST /user/profile/location
    // ==========================

    @Nested
    @DisplayName("POST /user/profile/location")
    class UpdateLocationTests {

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("создаёт новую локацию, если её не было")
        void updateLocation_createNew() throws Exception {
            LocationDto dto = new LocationDto();
            dto.setCountry("Russia");
            dto.setCity("Saint-Petersburg");
            dto.setLatitude(59.9343);
            dto.setLongitude(30.3351);

            mockMvc.perform(post("/user/profile/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            "Геолокация успешно сохранена longitude: 30.3351 / latitude: 59.9343"));

            User updated = userRepository.findByEmail("user@example.com").orElseThrow();
            assertThat(updated.getLocation()).isNotNull();
            assertThat(updated.getLocation().getCity()).isEqualTo("Saint-Petersburg");
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("заменяет существующую локацию новой и удаляет старую")
        void updateLocation_replaceExisting() throws Exception {
            Location oldLocation = Location.builder()
                    .country("Russia")
                    .city("Kazan")
                    .latitude(55.0)
                    .longitude(48.0)
                    .build();
            oldLocation = locationRepository.save(oldLocation);

            User existingUserDownload = userRepository.findByEmail(existingUser.getEmail()).orElseThrow();

            existingUserDownload.setLocation(oldLocation);
            userRepository.save(existingUserDownload);

            LocationDto dto = new LocationDto();
            dto.setCountry("Russia");
            dto.setCity("Sochi");
            dto.setLatitude(43.6);
            dto.setLongitude(39.7);

            mockMvc.perform(post("/user/profile/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            "Геолокация успешно сохранена longitude: 39.7 / latitude: 43.6"));

            User updated = userRepository.findByEmail("user@example.com").orElseThrow();
            assertThat(updated.getLocation()).isNotNull();
            assertThat(updated.getLocation().getCity()).isEqualTo("Sochi");

            // старая локация должна быть удалена
            assertThat(locationRepository.findById(oldLocation.getId())).isEmpty();
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("валидация LocationDto через @Valid → 400 при некорректных данных")
        void updateLocation_validationError() throws Exception {
            LocationDto dto = new LocationDto();
            // не заполняем ничего → ожидаем 400
            mockMvc.perform(post("/user/profile/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "unknown@example.com")
        @DisplayName("если пользователь не найден по email → 404")
        void updateLocation_userNotFound() throws Exception {
            LocationDto dto = new LocationDto();
            dto.setCountry("Russia");
            dto.setCity("Moscow");
            dto.setLatitude(55.7);
            dto.setLongitude(37.6);

            mockMvc.perform(post("/user/profile/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }
}
