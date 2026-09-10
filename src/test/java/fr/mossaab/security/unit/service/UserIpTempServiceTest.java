package fr.mossaab.security.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.entities.UserIpTemp;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.repository.UserIpTempRepository;
import fr.mossaab.security.service.UserIpTempService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserIpTempServiceTest {

    @Mock
    private UserIpTempRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IpHelper ipHelper;

    @InjectMocks
    private UserIpTempService userIpTempService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Проверка сохранения IP-адреса с валидными данными")
    void testSaveIpTemp_Success() {
        // Given
        Long userId = 1L;
        String ipAddress = "192.168.1.100";
        boolean isPrivateOrLoopback = true;

        UserIpTemp expectedEntry = new UserIpTemp();
        expectedEntry.setUserId(userId);
        expectedEntry.setIpAddress(ipAddress);
        expectedEntry.setIsPrivateOrLoopback(isPrivateOrLoopback);

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(expectedEntry);

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(any(UserIpTemp.class));
        verify(repository, times(1)).findAllByUserIdOrderByCreatedAtDesc(userId);
        verify(ipHelper, times(1)).isInternalIp(ipAddress);
    }

    @Test
    @DisplayName("Проверка удаления самого старого IP при превышении лимита в 5")
    void testSaveIpTemp_DeleteOldestWhenLimitExceeded() {
        // Given
        Long userId = 1L;
        String newIpAddress = "203.0.113.5";
        boolean isPrivateOrLoopback = false;

        // Создаем 5 существующих IP-адресов
        List<UserIpTemp> existingIps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UserIpTemp ip = new UserIpTemp();
            ip.setUserId(userId);
            ip.setIpAddress("192.168.1." + i);
            ip.setCreatedAt(Instant.now().minusSeconds(i * 60));
            existingIps.add(ip);
        }

        UserIpTemp newEntry = new UserIpTemp();
        newEntry.setUserId(userId);
        newEntry.setIpAddress(newIpAddress);

        // Mocking
        when(ipHelper.isInternalIp(newIpAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(existingIps);
        when(repository.save(any(UserIpTemp.class))).thenReturn(newEntry);

        // When
        userIpTempService.saveIpTemp(userId, newIpAddress);

        // Then
        verify(repository, times(1)).delete(existingIps.get(0)); // удаляем самый старый
        verify(repository, times(1)).save(any(UserIpTemp.class));
    }

    @Test
    @DisplayName("Проверка сохранения без удаления, если меньше 5 IP")
    void testSaveIpTemp_NoDeleteWhenLessThanFive() {
        // Given
        Long userId = 1L;
        String newIpAddress = "203.0.113.5";
        boolean isPrivateOrLoopback = false;

        // Создаем 3 существующих IP-адреса
        List<UserIpTemp> existingIps = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserIpTemp ip = new UserIpTemp();
            ip.setUserId(userId);
            ip.setIpAddress("192.168.1." + i);
            existingIps.add(ip);
        }

        UserIpTemp newEntry = new UserIpTemp();
        newEntry.setUserId(userId);
        newEntry.setIpAddress(newIpAddress);

        // Mocking
        when(ipHelper.isInternalIp(newIpAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(existingIps);
        when(repository.save(any(UserIpTemp.class))).thenReturn(newEntry);

        // When
        userIpTempService.saveIpTemp(userId, newIpAddress);

        // Then
        verify(repository, never()).delete(any());
        verify(repository, times(1)).save(any(UserIpTemp.class));
    }

    @Test
    @DisplayName("Проверка сохранения внешнего IP-адреса")
    void testSaveIpTemp_ExternalIp() {
        // Given
        Long userId = 1L;
        String ipAddress = "203.0.113.5";
        boolean isPrivateOrLoopback = false;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                ipAddress.equals(entry.getIpAddress()) && 
                !entry.getIsPrivateOrLoopback()
        ));
    }

    @Test
    @DisplayName("Проверка сохранения loopback IP-адреса")
    void testSaveIpTemp_Loopback() {
        // Given
        Long userId = 1L;
        String ipAddress = "127.0.0.1";
        boolean isPrivateOrLoopback = true;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                entry.getIsPrivateOrLoopback()
        ));
    }

    @Test
    @DisplayName("Проверка сохранения IPv6 loopback")
    void testSaveIpTemp_Ipv6Loopback() {
        // Given
        Long userId = 1L;
        String ipAddress = "::1";
        boolean isPrivateOrLoopback = true;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                entry.getIsPrivateOrLoopback() && "::1".equals(entry.getIpAddress())
        ));
    }

    @Test
    @DisplayName("Проверка сохранения null IP-адреса")
    void testSaveIpTemp_Null() {
        // Given
        Long userId = 1L;
        String ipAddress = null;

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, never()).save(any(UserIpTemp.class));
        verify(repository, never()).findAllByUserIdOrderByCreatedAtDesc(anyLong());
        verify(ipHelper, never()).isInternalIp(anyString());
    }

    @Test
    @DisplayName("Проверка сохранения пустого IP-адреса")
    void testSaveIpTemp_Empty() {
        // Given
        Long userId = 1L;
        String ipAddress = "";

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, never()).save(any(UserIpTemp.class));
        verify(repository, never()).findAllByUserIdOrderByCreatedAtDesc(anyLong());
        verify(ipHelper, never()).isInternalIp(anyString());
    }

    @Test
    @DisplayName("Проверка сохранения blank IP-адреса")
    void testSaveIpTemp_Blank() {
        // Given
        Long userId = 1L;
        String ipAddress = "   ";

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, never()).save(any(UserIpTemp.class));
        verify(repository, never()).findAllByUserIdOrderByCreatedAtDesc(anyLong());
        verify(ipHelper, never()).isInternalIp(anyString());
    }

    @Test
    @DisplayName("Проверка получения IP-адресов пользователя")
    void testGetTrackedIpForUser() {
        // Given
        Long userId = 1L;
        UserIpTemp userIpTemp1 = new UserIpTemp();
        userIpTemp1.setIpAddress("192.168.1.100");
        userIpTemp1.setCreatedAt(Instant.now());
        
        UserIpTemp userIpTemp2 = new UserIpTemp();
        userIpTemp2.setIpAddress("10.0.0.50");
        userIpTemp2.setCreatedAt(Instant.now().minusSeconds(100));

        UserIpTempDto dto1 = new UserIpTempDto();
        dto1.setIpAddress("192.168.1.100");
        
        UserIpTempDto dto2 = new UserIpTempDto();
        dto2.setIpAddress("10.0.0.50");

        // Mocking
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(userIpTemp1, userIpTemp2));
        when(objectMapper.convertValue(userIpTemp1, UserIpTempDto.class)).thenReturn(dto1);
        when(objectMapper.convertValue(userIpTemp2, UserIpTempDto.class)).thenReturn(dto2);

        // When
        List<UserIpTempDto> result = userIpTempService.getTrackedIpForUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("192.168.1.100", result.get(0).getIpAddress());
        assertEquals("10.0.0.50", result.get(1).getIpAddress());

        verify(repository, times(1)).findAllByUserIdOrderByCreatedAtDesc(userId);
        verify(objectMapper, times(1)).convertValue(userIpTemp1, UserIpTempDto.class);
        verify(objectMapper, times(1)).convertValue(userIpTemp2, UserIpTempDto.class);
    }

    @Test
    @DisplayName("Проверка получения пустого списка IP-адресов")
    void testGetTrackedIpForUser_Empty() {
        // Given
        Long userId = 1L;

        // Mocking
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        // When
        List<UserIpTempDto> result = userIpTempService.getTrackedIpForUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Проверка ограничения возвращаемых IP до 5 штук")
    void testGetTrackedIpForUser_LimitToFive() {
        // Given
        Long userId = 1L;
        List<UserIpTemp> existingIps = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserIpTemp ip = new UserIpTemp();
            ip.setUserId(userId);
            ip.setIpAddress("192.168.1." + i);
            ip.setCreatedAt(Instant.now().minusSeconds(i * 60));
            existingIps.add(ip);
        }

        List<UserIpTempDto> dtos = existingIps.stream()
                .map(ip -> UserIpTempDto.builder().ipAddress(ip.getIpAddress()).build())
                .toList();

        // Mocking
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(existingIps);
        when(objectMapper.convertValue(any(UserIpTemp.class), eq(UserIpTempDto.class)))
                .thenAnswer(invocation -> {
                    UserIpTemp ip = invocation.getArgument(0);
                    return UserIpTempDto.builder().ipAddress(ip.getIpAddress()).build();
                });

        // When
        List<UserIpTempDto> result = userIpTempService.getTrackedIpForUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(5, result.size()); // должно быть не больше 5
    }

    @Test
    @DisplayName("Проверка сохранения приватного диапазона 10.x.x.x")
    void testSaveIpTemp_PrivateRange_10() {
        // Given
        Long userId = 1L;
        String ipAddress = "10.0.0.1";
        boolean isPrivateOrLoopback = true;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                entry.getIsPrivateOrLoopback() && "10.0.0.1".equals(entry.getIpAddress())
        ));
    }

    @Test
    @DisplayName("Проверка сохранения приватного диапазона 192.168.x.x")
    void testSaveIpTemp_PrivateRange_192() {
        // Given
        Long userId = 1L;
        String ipAddress = "192.168.1.1";
        boolean isPrivateOrLoopback = true;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                entry.getIsPrivateOrLoopback() && "192.168.1.1".equals(entry.getIpAddress())
        ));
    }

    @Test
    @DisplayName("Проверка сохранения приватного диапазона 172.16-31.x.x (Docker)")
    void testSaveIpTemp_PrivateRange_172() {
        // Given
        Long userId = 1L;
        String ipAddress = "172.16.0.1";
        boolean isPrivateOrLoopback = true;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                entry.getIsPrivateOrLoopback() && "172.16.0.1".equals(entry.getIpAddress())
        ));
    }

    @Test
    @DisplayName("Проверка сохранения loopback 127.x.x.x")
    void testSaveIpTemp_Loopback_127() {
        // Given
        Long userId = 1L;
        String ipAddress = "127.0.0.5";
        boolean isPrivateOrLoopback = true;

        // Mocking
        when(ipHelper.isInternalIp(ipAddress)).thenReturn(isPrivateOrLoopback);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(repository.save(any(UserIpTemp.class))).thenReturn(new UserIpTemp());

        // When
        userIpTempService.saveIpTemp(userId, ipAddress);

        // Then
        verify(repository, times(1)).save(argThat(entry -> 
                entry.getIsPrivateOrLoopback() && "127.0.0.5".equals(entry.getIpAddress())
        ));
    }
}
