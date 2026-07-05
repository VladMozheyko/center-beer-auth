package fr.mossaab.security.unit.service;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import fr.mossaab.security.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-test для сервиса UserService")
class UserServiceTest {


    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileDataRepository fileDataRepository;

    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;

    private User userWithTempEmail;
    private User userWithoutTempEmail;

    @BeforeEach
    void setUp() {
        userWithTempEmail = User.builder()
                .id(1L)
                .email("current@example.com")
                .tempEmail("new@example.com")
                .activationCode("testCode")
                .build();

        userWithoutTempEmail = User.builder()
                .id(2L)
                .email("another@example.com")
                .tempEmail(null)
                .activationCode("anotherCode")
                .build();
    }

    @Nested
    @DisplayName("Тестирование функциональности метода getFullUserInfo")
    class GetFullUserInfo {

        @Test
        @DisplayName("Пользователь не найден — ошибка RuntimeException")
        void getFullUserInfo_UserNotFound_ExceptionThrown() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> {
                userService.getFullUserInfo(1L);
            });

            verify(userRepository, times(1)).findById(1L);
            verifyNoMoreInteractions(userRepository);
            verifyNoInteractions(fileDataRepository);
            verifyNoInteractions(userSocialAccountRepository);
        }

        @Test
        @DisplayName("Пользователь найден, корректно собран ответ с файлом и одной соцсетью")
        void getFullUserInfo_UserFound_WithFileAndSocialAccount_Success() {
            User user = User.builder()
                    .id(1L)
                    .nickname("johnDoe")
                    .email("john@example.com")
                    .phone("+79001112233")
                    .build();

            FileData fileData = FileData.builder()
                    .name("avatar.png")
                    .filePath("/uploads/avatars/avatar.png")
                    .build();

            UserSocialAccount vkAccount = UserSocialAccount.builder()
                    .user(user)
                    .provider(OAuthProvider.VK)
                    .socialEmail("vk_user@example.com")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(fileDataRepository.findByUserId(1L)).thenReturn(Optional.of(fileData));
            when(userSocialAccountRepository.findByUserId(1L)).thenReturn(List.of(vkAccount));

            var response = userService.getFullUserInfo(1L);

            assertNotNull(response);
            assertEquals("johnDoe", response.getUserName());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("+79001112233", response.getPhone());
            assertNotNull(response.getFileData());
            assertEquals("avatar.png", response.getFileData().getName());
            assertEquals("/uploads/avatars/avatar.png", response.getFileData().getPath());
            assertNotNull(response.getSocialAccounts());
            assertEquals(1, response.getSocialAccounts().size());

            var vkResponse = response.getSocialAccounts().get(0);
            assertEquals(OAuthProvider.VK, vkResponse.getProvider());
            assertEquals("vk_user@example.com", vkResponse.getSocialEmail());

            verify(userRepository, times(1)).findById(1L);
            verify(fileDataRepository, times(1)).findByUserId(1L);
            verify(userSocialAccountRepository, times(1)).findByUserId(1L);
        }
    }

    @Nested
    @DisplayName("Тестирование функциональности метода confirmEmailChange")
    class ConfirmEmailChange {

        @Test
        @DisplayName("Успешное изменение email")
        void confirmEmailChange_Success() {
            when(userRepository.findByActivationCode("testCode")).thenReturn(Optional.of(userWithTempEmail));
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

            boolean result = userService.confirmEmailChange("testCode");

            assertTrue(result);
            verify(userRepository, times(1)).saveAndFlush(userWithTempEmail);
            assertNull(userWithTempEmail.getTempEmail());
        }

        @Test
        @DisplayName("Код активации не найден — ошибка IllegalArgumentException")
        void confirmEmailChange_InvalidCode_ExceptionThrown() {
            when(userRepository.findByActivationCode("invalidCode")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                userService.confirmEmailChange("invalidCode");
            });
        }

        @Test
        @DisplayName("У пользователя отсутствует tempEmail — ошибка IllegalStateException")
        void confirmEmailChange_NoTempEmail_ExceptionThrown() {
            when(userRepository.findByActivationCode("anotherCode")).thenReturn(Optional.of(userWithoutTempEmail));

            assertThrows(IllegalStateException.class, () -> {
                userService.confirmEmailChange("anotherCode");
            });
        }

        @Test
        @DisplayName("Новый email уже существует — ошибка DuplicateResourceException")
        void confirmEmailChange_EmailAlreadyExists_ExceptionThrown() {
            when(userRepository.findByActivationCode("testCode")).thenReturn(Optional.of(userWithTempEmail));
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(new User()));

            assertThrows(DuplicateResourceException.class, () -> {
                userService.confirmEmailChange("testCode");
            });
        }
    }

    @Nested
    @DisplayName("Тестирование функциональности метода getNewEmailForConfirmation")
    class GetNewEmailForConfirmation {

        @Test
        @DisplayName("Возвращается tempEmail, если код найден")
        void getNewEmailForConfirmation_CodeExists_ReturnTempEmail() {
            when(userRepository.findByActivationCode("testCode")).thenReturn(Optional.of(userWithTempEmail));

            String tempEmail = userService.getNewEmailForConfirmation("testCode");

            assertEquals("new@example.com", tempEmail);
        }

        @Test
        @DisplayName("Возвращается null, если код не найден")
        void getNewEmailForConfirmation_CodeNotExists_ReturnNull() {
            when(userRepository.findByActivationCode("nonexistentCode")).thenReturn(Optional.empty());

            String tempEmail = userService.getNewEmailForConfirmation("nonexistentCode");

            assertNull(tempEmail);
        }
    }

    @Nested
    @DisplayName("Тестирование функциональности метода deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("Удаление пользователя, если он существует")
        void deleteUser_UserExists_SuccessfulDeletion() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Пользователь не найден — ошибка IllegalArgumentException")
        void deleteUser_UserNotFound_ExceptionThrown() {
            when(userRepository.existsById(1L)).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> {
                userService.deleteUser(1L);
            });
        }
    }
}