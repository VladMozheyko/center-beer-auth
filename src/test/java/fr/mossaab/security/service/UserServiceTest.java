package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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