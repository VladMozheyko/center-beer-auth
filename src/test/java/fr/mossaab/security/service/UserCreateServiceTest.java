package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests для UserCreateService")
class UserCreateServiceTest {

    @InjectMocks
    private UserCreateService userCreateService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("Создание админа, если пользовательская база пуста")
    void createUsers_EmptyDatabase_CreatesUser() {
        when(userRepository.count()).thenReturn(0L);

        userCreateService.createUsers();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("База пользователей не пуста - пользователь не создается")
    void createUsers_NonEmptyDatabase_DoesNotCreateUser() {
        when(userRepository.count()).thenReturn(1L);

        userCreateService.createUsers();

        verify(userRepository, never()).save(any(User.class));
    }
}