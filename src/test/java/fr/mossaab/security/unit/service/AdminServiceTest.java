package fr.mossaab.security.unit.service;

import fr.mossaab.security.dto.user.GetAllUsersResponse;
import fr.mossaab.security.dto.user.GetUsersDto;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-test для сервиса AdminService")
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    private User user1; // активированный пользователь с ролью
    private User user2; // неактивированный пользователь без роли
    private User user3; // пользователь с полностью null-полями

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .nickname("Vasja")
                .email("vasja@example.ru")
                .activationCode(null)
                .role(Role.ADMIN)
                .build();
        user2 = User.builder()
                .id(2L)
                .nickname("Masha")
                .email("masha@example.ru")
                .activationCode("code123")
                .role(null)
                .build();
        user3 = User.builder()
                .id(null)
                .nickname(null)
                .email(null)
                .activationCode(null)
                .role(null)
                .build();
    }

    @Test
    @DisplayName("Возврат пагинированного списка пользователей")
    void getAllUsers_WhenUsersExist_ShouldReturnPaginatedList() {
        List<User> users = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        GetAllUsersResponse response = adminService.getAllUsers(0, 1);

        verify(userRepository, times(1)).findAll();
        assertEquals("success", response.getStatus());
        assertEquals("Пользователи получены", response.getNotify());
        assertEquals(1, response.getUsers().size());
        assertEquals(users.size(), response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertEquals(1, response.getPageSize());
    }

    @Test
    @DisplayName("Пустой список пользователей, когда их нет")
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        GetAllUsersResponse response = adminService.getAllUsers(0, 5);

        verify(userRepository, times(1)).findAll();
        assertEquals("success", response.getStatus());
        assertEquals(0, response.getUsers().size());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("Признак последней страницы должен быть true на последней странице")
    void getAllUsers_WhenLastPage_ShouldSetLastFieldTrue() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        GetAllUsersResponse response = adminService.getAllUsers(1, 1);

        verify(userRepository, times(1)).findAll();
        assertEquals(1, response.getUsers().size());
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
        assertEquals(2, response.getTotalPages());
        assertEquals(1, response.getPageNumber());
        assertEquals(2, response.getTotalElements());
    }

    @Test
    @DisplayName("Возврат null для email и id, если они отсутствуют у пользователя")
    void getAllUsers_WhenUserHasNoEmailAndId_ShouldReturnNullInDto() {
        when(userRepository.findAll()).thenReturn(List.of(user3));

        GetAllUsersResponse response = adminService.getAllUsers(0, 5);

        verify(userRepository, times(1)).findAll();
        assertEquals(1, response.getUsers().size());
        GetUsersDto usersDto = response.getUsers().get(0);
        assertNull(usersDto.getEmail());
        assertNull(usersDto.getId());
        assertTrue(usersDto.getActivationCode()); // activationCode == null => true
        assertNull(usersDto.getRole());
    }

    @Test
    @DisplayName("Возврат всех пользователей, когда размер страницы больше количества пользователей")
    void getAllUsers_WhenPageSizeGreaterThanUserCount_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        GetAllUsersResponse response = adminService.getAllUsers(0, 10);

        verify(userRepository, times(1)).findAll();
        assertEquals(2, response.getUsers().size());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("Проверка, что при размере страницы равном количеству пользователей возвращается одна страница")
    void getAllUsers_WhenPageSizeEqualsUserCount_ShouldReturnSingleFullPage() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        GetAllUsersResponse response = adminService.getAllUsers(0, 2);

        verify(userRepository, times(1)).findAll();
        assertEquals(2, response.getUsers().size());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("Исключение при обращении к несуществующей странице")
    void getAllUsers_WhenPageOutOfBounds_ShouldThrowIllegalArgumentException() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        assertThrows(IllegalArgumentException.class, () -> adminService.getAllUsers(10, 2));
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Корректное вычисление offset и pageNumber")
    void getAllUsers_ShouldCalculateOffsetAndPageNumberCorrectly() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user2));

        GetAllUsersResponse response = adminService.getAllUsers(1, 2);

        verify(userRepository, times(1)).findAll();
        assertEquals(2, response.getOffset());
        assertEquals(1, response.getPageNumber());
    }

    @Test
    @DisplayName("Проверка поля activationCode: если не null, возвращается false")
    void getAllUsers_WhenUserHasActivationCode_ShouldSetActivationCodeFalse() {
        when(userRepository.findAll()).thenReturn(List.of(user2));

        GetAllUsersResponse response = adminService.getAllUsers(0, 5);

        verify(userRepository, times(1)).findAll();
        GetUsersDto dto = response.getUsers().get(0);
        assertFalse(dto.getActivationCode());
    }

    @Test
    @DisplayName("Корректное возвращение значения роли, если роль задана")
    void getAllUsers_WhenUserHasRole_ShouldReturnRoleName() {
        when(userRepository.findAll()).thenReturn(List.of(user1));

        GetAllUsersResponse response = adminService.getAllUsers(0, 5);

        verify(userRepository, times(1)).findAll();
        GetUsersDto dto = response.getUsers().get(0);
        assertEquals(Role.ADMIN.name(), dto.getRole());
    }


    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    @DisplayName("Передача отрицательного размера страницы — должно бросать IllegalArgumentException")
    void getAllUsers_WhenPageSizeNegative_ShouldThrowIllegalArgumentException(int size) {
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        assertThrows(IllegalArgumentException.class, () -> adminService.getAllUsers(0, size));
    }

    @Test
    @DisplayName("Передача нулевого размера страницы — должно dto без пользователей")
    void getAllUsers_WhenPageSizeZero_ShouldReturnEmptyUserList() {
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        GetAllUsersResponse uu = adminService.getAllUsers(0, 0);
        assertNotNull(uu);
        assertEquals(0, uu.getUsers().size());
    }

}