package fr.mossaab.security.unit.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.UserStatus;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.PendingUserCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты PendingUserCleanupService")
class PendingUserCleanupServiceTest {

    @InjectMocks
    private PendingUserCleanupService cleanupService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("markExpiredPendingUsers: помечает неактивированные аккаунты старше 24 часов")
    void markExpiredPendingUsers_ShouldMarkExpired() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        User pendingUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testuser")
                .status(UserStatus.PENDING)
                .createdAt(cutoff.minusHours(1))
                .build();

        when(userRepository.findByStatusAndCreatedAtBefore(eq(UserStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingUser));

        // When
        cleanupService.markExpiredPendingUsers();

        // Then
        assertEquals(UserStatus.EXPIRED, pendingUser.getStatus());
        verify(userRepository, times(1)).save(pendingUser);
    }

    @Test
    @DisplayName("markExpiredPendingUsers: не помечает аккаунты младше 24 часов")
    void markExpiredPendingUsers_ShouldNotMarkRecent() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        User recentUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testuser")
                .status(UserStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByStatusAndCreatedAtBefore(eq(UserStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        cleanupService.markExpiredPendingUsers();

        // Then
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteExpiredUsers: удаляет EXPIRED аккаунты старше 30 дней")
    void deleteExpiredUsers_ShouldDeleteOld() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        when(userRepository.deleteByStatusAndCreatedAtBefore(eq(UserStatus.EXPIRED), any(LocalDateTime.class)))
                .thenReturn(5);

        // When
        cleanupService.deleteExpiredUsers();

        // Then
        verify(userRepository, times(1)).deleteByStatusAndCreatedAtBefore(eq(UserStatus.EXPIRED), any(LocalDateTime.class));
    }
}
