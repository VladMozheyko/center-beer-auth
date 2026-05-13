package fr.mossaab.security.unit.controller;

import fr.mossaab.security.controller.UserController;
import fr.mossaab.security.dto.user.LocationDto;
import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.entities.Location;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.LocationRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.UserService;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests for UserController")
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailSender mailSender;

    @Mock
    private UserService userService;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    @DisplayName("Получение профиля пользователя - успешное")
    void getSimpleProfile_Success() {
        User user = User.builder().id(1L).email("test@example.com").nickname("testUser").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<UserProfileResponse> response = userController.getSimpleProfile();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("testUser", Objects.requireNonNull(response.getBody()).getNickname());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Обновление никнейма - успешное")
    void updateNickname_Success() {
        User user = User.builder().email("test@example.com").nickname("oldNickname").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByNickname("newNickname")).thenReturn(Optional.empty());

        ResponseEntity<String> response = userController.updateNickname("newNickname");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Никнейм успешно обновлён на newNickname", response.getBody());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Запрос на смену email - успешный")
    void requestEmailChange_Success() {
        User user = User.builder().email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        String newEmail = "new@example.com";
        ResponseEntity<String> response = userController.requestEmailChange(newEmail);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Ссылка для подтверждения отправлена на " + newEmail, response.getBody());
        verify(mailSender, times(1)).send(eq(newEmail), anyString(), anyString());
    }

    @Test
    @DisplayName("Подтверждение смены email - успешное")
    void confirmEmailChange_Success() {
        // Подготовка данных
        String code = "validCode";
        boolean expectedResult = true;

        // Настройка мока
        when(userService.confirmEmailChange(code)).thenReturn(expectedResult);

        // Выполнение теста
        ResponseEntity<String> response = userController.confirmEmailChange(code);

        // Валидация результатов
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("E-mail успешно изменён", response.getBody());

        // Проверка вызова метода
        verify(userService).confirmEmailChange(code);
    }

    @Test
    @DisplayName("Удаление аккаунта - успешное")
    void deleteAccount_Success() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<String> response = userController.deleteAccount();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Аккаунт успешно удалён", response.getBody());
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("Сохранение геолокации - успешное")
    void updateMyLocation_Success() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LocationDto locationDto = new LocationDto(40.7128, -74.0060, "USA", "New York");
        ResponseEntity<?> response = userController.updateMyLocation(locationDto);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Обновление никнейма - никнейм уже существует")
    void updateNickname_NicknameExists_ThrowsException() {
        User user = User.builder().email("test@example.com").nickname("oldNickname").build();
        User existingUser = User.builder().id(2L).nickname("existingNickname").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByNickname("existingNickname")).thenReturn(Optional.of(existingUser));

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () ->
                userController.updateNickname("existingNickname"));

        assertEquals("Пользователь с таким никнеймом уже существует", exception.getMessage());
    }
}