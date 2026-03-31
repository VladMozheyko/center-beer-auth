package fr.mossaab.security.service.social;

import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;

import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests for OAuth2SocialCallbackHandler")
class OAuth2SocialCallbackHandlerTest {

    @InjectMocks
    private OAuth2SocialCallbackHandler successHandler;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    private OAuth2User oauth2User;

    @BeforeEach
    void setUp() {
        Map<String, Object> attributes = Map.of(
                "email", "test@example.com",
                "name", "Test User"
        );
        oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new OAuth2UserAuthority(attributes)),
                attributes, "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);
    }

    @Nested
    @DisplayName("Успешная аутентификация существующего пользователя")
    class ExistingUser {

        @Test
        @DisplayName("Обновление куки и отправка JSON")
        void authenticationSuccess_ExistingUser_UpdatesCookiesAndSendsJson() throws Exception {
            User existingUser = User.builder()
                    .email("test@example.com")
                    .role(Role.USER)
                    .build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(existingUser)).thenReturn("jwtToken");
            when(refreshTokenService.createRefreshToken(existingUser.getId())).thenReturn(new RefreshToken(existingUser.getId(), existingUser, "refreshToken", null, false));
            ResponseCookie jwtCookie = ResponseCookie.from("jwt", "jwtToken").build();
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "refreshToken").build();
            when(jwtService.generateJwtCookie("jwtToken")).thenReturn(jwtCookie);
            when(refreshTokenService.generateRefreshTokenCookie("refreshToken")).thenReturn(refreshTokenCookie);

            PrintWriter writer = mock(PrintWriter.class);
            when(response.getWriter()).thenReturn(writer);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(response).addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            verify(response).addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
            verify(writer).write(contains("\"accessToken\": \"jwtToken\""));
        }
    }

    @Nested
    @DisplayName("Успешная аутентификация нового пользователя")
    class NewUser {

        @Test
        @DisplayName("Создание пользователя, обновление куки и отправка JSON")
        void authenticationSuccess_NewUser_CreatesUserAndUpdatesCookiesAndSendsJson() throws Exception {
            User newUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("Test_User_12345")
                    .role(Role.USER)
                    .build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(newUser));
            when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");
            when(refreshTokenService.createRefreshToken(newUser.getId())).thenReturn(new RefreshToken(newUser.getId(), newUser, "refreshToken", null, false));

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", "jwtToken").build();
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "refreshToken").build();
            when(jwtService.generateJwtCookie("jwtToken")).thenReturn(jwtCookie);
            when(refreshTokenService.generateRefreshTokenCookie("refreshToken")).thenReturn(refreshTokenCookie);

            PrintWriter writer = mock(PrintWriter.class);
            when(response.getWriter()).thenReturn(writer);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(response).addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            verify(response).addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
            verify(writer).write(contains("\"accessToken\": \"jwtToken\""));
        }
    }

    @Nested
    @DisplayName("Обработка исключений")
    class ExceptionHandling {

        @Test
        @DisplayName("Сценарий генерации токена завершается с выведением ошибки")
        void shouldHandleRoleNotFound() throws Exception {
            User user = User.builder().email("test@example.com").role(Role.USER).build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            // Мокируем исключение
            when(jwtService.generateToken(user)).thenThrow(new IllegalArgumentException("Role not found"));

            Exception ex = assertThrows(RuntimeException.class, () -> successHandler.onAuthenticationSuccess(request, response, authentication));

            assertEquals("Role not found", ex.getMessage());
            verify(refreshTokenService, never()).createRefreshToken(any());
            verify(jwtService, never()).generateJwtCookie("jwtToken");
            verify(refreshTokenService, never()).generateRefreshTokenCookie(any());
        }
    }

    @Nested
    @DisplayName("Тестирование наличия e-mail атрибута")
    class MissingEmailAttribute {

        @BeforeEach
        void setUp() {
            Map<String, Object> attributes = Map.of(
                    "name", "Test User"
            );
            oauth2User = new DefaultOAuth2User(
                    Collections.singletonList(new OAuth2UserAuthority(attributes)),
                    attributes, "name"
            );
            when(authentication.getPrincipal()).thenReturn(oauth2User);
        }

        @Test
        @DisplayName("Создание пользователя без email")
        void shouldCreateUserWithoutEmail() throws Exception {
            when(userRepository.findByEmail(null)).thenReturn(Optional.empty());
            User newUser = User.builder()
                    .id(1L)
                    .email("test@example.com") // Поскольку email отсутствует
                    .nickname("Test_User_12345")
                    .role(Role.USER)
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");
            when(refreshTokenService.createRefreshToken(null)).thenReturn(new RefreshToken(1L, newUser, "refreshToken", null, false));

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", "jwtToken").build();
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "refreshToken").build();
            when(jwtService.generateJwtCookie("jwtToken")).thenReturn(jwtCookie);
            when(refreshTokenService.generateRefreshTokenCookie("refreshToken")).thenReturn(refreshTokenCookie);

            PrintWriter writer = mock(PrintWriter.class);
            when(response.getWriter()).thenReturn(writer);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(userRepository).save(any(User.class)); // Проверка вызова сохранения нового пользователя
            verify(response).addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            verify(response).addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
            verify(writer).write(contains("\"accessToken\": \"jwtToken\""));
        }
    }
}