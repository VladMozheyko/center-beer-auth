package fr.mossaab.security.unit.service;

import fr.mossaab.security.dto.auth.PhoneRegisterRequest;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.RegistrationMethod;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.PhoneRegistrationFacade;
import fr.mossaab.security.service.PhoneVerificationService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.UserIpTempService;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.builder.AuthenticationResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests для PhoneRegistrationFacade")
class PhoneRegistrationFacadeTest {

    @InjectMocks
    private PhoneRegistrationFacade phoneRegistrationFacade;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private IpHelper helper;

    @Mock
    private UserIpTempService userIpTempService;

    @Mock
    private AuthenticationResponseBuilder authenticationResponseBuilder;


    @Test
    @DisplayName("Начало регистрации с телефоном - успешное")
    void start_ValidRequest_Success() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        PhoneRegisterRequest request = PhoneRegisterRequest.builder()
                .nickname("testUser")
                .email("test@example.com")
                .password("password123")
                .phone("1234567890")
                .method(RegistrationMethod.SMS)
                .build();
        when(phoneVerificationService.sendCode("1234567890", RegistrationMethod.SMS)).thenReturn("123456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // Set an ID since the mock doesn't do it
            if (user.getId() == null) {
                user.setId(1L);
            }
            return user;
        });
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        when(helper.getClientIp(mockRequest)).thenReturn("127.0.0.1");
        doNothing().when(userIpTempService).saveIpTemp(anyLong(), anyString());
        when(authenticationResponseBuilder.getIpHelper()).thenReturn(helper);

        phoneRegistrationFacade.start(request, mockRequest);

        verify(userRepository, times(1)).save(any(User.class)); // один раз для создания, другой для обновления кода
        verify(phoneVerificationService, times(1)).sendCode(eq("1234567890"), eq(RegistrationMethod.SMS));
        verify(helper, times(1)).getClientIp(mockRequest);
        verify(userIpTempService, times(1)).saveIpTemp(anyLong(), anyString());
    }

    @Test
    @DisplayName("Подтверждение телефона - успешное")
    void confirm_ValidPhoneAndCode_Success() {
        User user = User.builder()
                .phone("1234567890")
                .phoneActivationCode("123456")
                .build();

        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(user));

        phoneRegistrationFacade.confirm("1234567890", "123456");

        assertNull(user.getPhoneActivationCode());
        assertTrue(user.getPhoneVerified());
    }

    @Test
    @DisplayName("Подтверждение телефона - неверный код")
    void confirm_WrongCode_ThrowsException() {
        User user = User.builder()
                .phone("1234567890")
                .phoneActivationCode("123456")
                .build();

        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                phoneRegistrationFacade.confirm("1234567890", "wrongCode"));

        assertEquals("Wrong code", exception.getMessage());
    }

    @Test
    @DisplayName("Подтверждение телефона - пользователь не найден")
    void confirm_UserNotFound_ThrowsException() {
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                phoneRegistrationFacade.confirm("1234567890", "123456"));

        assertEquals("User not found", exception.getMessage());
    }
}
