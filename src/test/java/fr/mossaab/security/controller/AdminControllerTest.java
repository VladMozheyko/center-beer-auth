package fr.mossaab.security.controller;

import fr.mossaab.security.dto.user.GetAllUsersResponse;
import fr.mossaab.security.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests для AdminController")
public class AdminControllerTest {

    @InjectMocks
    private AdminController adminController;

    @Mock
    private AdminService adminService;

    @Test
    @DisplayName("Получение всех пользователей без ошибок")
    void getAllUsers_ReturnsUsersListSuccessfully() {
        GetAllUsersResponse mockResponse = new GetAllUsersResponse(); // Пример ответа
        when(adminService.getAllUsers(0, 10)).thenReturn(mockResponse);

        ResponseEntity<GetAllUsersResponse> response = adminController.getAllUsers(0, 10);

        assertEquals(ResponseEntity.ok(mockResponse), response);
        verify(adminService, times(1)).getAllUsers(0, 10);
    }
}