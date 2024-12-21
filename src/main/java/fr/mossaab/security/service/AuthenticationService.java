package fr.mossaab.security.service;


import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mossaab.security.controller.AuthController;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Реализация интерфейса AuthenticationService.
 * Обеспечивает аутентификацию пользователей и их регистрацию.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final FileDataRepository fileDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final MailSender mailSender;
    private final StorageService storageService;

    /**
     * Регистрирует нового пользователя.
     *
     * @param request Запрос на регистрацию.
     * @return Ответ с данными пользователя и токенами.
     * @throws ParseException В случае ошибки парсинга даты.
     */
    public AuthenticationResponse register(RegisterRequest request, MultipartFile image) throws IOException, ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .dateOfBirth(format.parse(request.getDateOfBirth()))
                .phoneNumber(request.getPhoneNumber())
                .build();
        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);
        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в Kotitonttu. Ваша ссылка для активации: http://31.129.102.70:8080/authentication/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Ссылка активации Kotitonttu", message);
        }
        user = userRepository.save(user);
        var jwt = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image,user);
        fileDataRepository.save(uploadImage);
        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .email(user.getEmail())
                .id(user.getId())
                .refreshToken(refreshToken.getToken())
                .roles(roles)
                .tokenType(TokenType.BEARER.name())
                .build();
    }

    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Пользователь с email " + email + " не найден");
        }

        User user = userOptional.get();
        String activationCode = UUID.randomUUID().toString(); // Генерация нового кода
        user.setActivationCode(activationCode);
        userRepository.save(user);

        String message = String.format(
                "Здравствуйте, %s! \n" +
                        "Ваша ссылка для смены пароля: http://31.129.102.70:8080/authentication/activate/%s",
                user.getUsername(),
                user.getActivationCode()
        );

        mailSender.send(user.getEmail(), "Код смены пароля в Kotitonttu", message);
    }
    public ResponseEntity<Void> refreshTokenUsingCookie(HttpServletRequest request) {
        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);
        RefreshTokenResponse refreshTokenResponse = refreshTokenService
                .generateNewToken(new RefreshTokenRequest(refreshToken));
        ResponseCookie newJwtCookie = jwtService.generateJwtCookie(refreshTokenResponse.getAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newJwtCookie.toString())
                .build();
    }
    public ResponseEntity<Object> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByActivationCode(request.getCode());
        if (user == null) {
            return new ResponseEntity<>("Код активации не найден", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setActivationCode(null); // Удаление использованного кода
        userRepository.save(user);

        return new ResponseEntity<>("Пароль успешно изменен", HttpStatus.OK);
    }
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        ResponseCookie jwtCookie = jwtService.getCleanJwtCookie();
        ResponseCookie refreshTokenCookie = refreshTokenService.getCleanRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .build();
    }

    public void  resendActivationCode(String email) throws ParseException {
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user = userOptional.get();
        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);
        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в Kotitonttu. Ваш ссылка активации: http://31.129.102.70:8080/authentication/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Ссылка активации Kotitonttu", message);
        }
        userRepository.save(user);
    }

    /**
     * Аутентифицирует пользователя.
     *
     * @param request Запрос на аутентификацию.
     * @return Ответ с данными пользователя и токенами.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();
        var jwt = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(jwt);
        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(refreshToken.getToken());

        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .roles(roles)
                .email(user.getEmail())
                .id(user.getId())
                .refreshToken(refreshToken.getToken())
                .tokenType(TokenType.BEARER.name())
                .jwtCookie(jwtCookie.toString())
                .refreshTokenCookie(refreshTokenCookie.toString())
                .build();
    }

    public synchronized boolean activateUser(String code) {
        User userEntity = userRepository.findByActivationCode(code);
        if (userEntity == null) {
            throw new NullPointerException("Пользователь с таким кодом активации не найден ");
        }
        if (Objects.equals(code, userEntity.getActivationCode())) {
            userEntity.setActivationCode(null);
            userRepository.save(userEntity);
            return true;
        } else {
            throw new NullPointerException("Введенный код не совпадает с истинным");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {

        /**
         * Токен обновления.
         */
        private String refreshToken;

    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivateResponse {
        private String status;
        private String notify;
        private String answer;
        private ErrorActivateDto errors;

        // Геттеры и сеттеры
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorActivateDto {
        private String code;

        // Геттеры и сеттеры
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationResponse {

        /**
         * Уникальный идентификатор пользователя.
         */
        private Long id;

        /**
         * Электронная почта пользователя.
         */
        private String email;

        /**
         * Список ролей пользователя.
         */
        private List<String> roles;

        /**
         * Токен доступа.
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * Токен обновления.
         */
        @JsonProperty("refresh_token")
        private String refreshToken;

        /**
         * Тип токена.
         */
        @JsonProperty("token_type")
        private String tokenType;

        private String jwtCookie;
        private String refreshTokenCookie;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {

        /**
         * Имя пользователя.
         */
        @Schema(description = "Имя пользователя", example = "Александр")
        private String firstname;

        /**
         * Фамилия пользователя.
         */
        @Schema(description = "Фамилия пользователя", example = "Иванов")
        private String lastname;

        /**
         * Электронная почта пользователя.
         */
        @Schema(description = "Почтовый адрес пользователя", example = "example@gmail.ru")
        private String email;

        /**
         * Пароль пользователя.
         */
        @Schema(description = "Пароль пользователя", example = "Sasha123!")
        private String password;



        /**
         * Дата рождения пользователя в формате строки.
         */
        @Schema(description = "Дата рождения", example = "2000-01-01")
        private String dateOfBirth;

        /**
         * Номер телефона пользователя.
         */
        @Schema(description = "Номер телефона", example = "+78005553555")
        private String phoneNumber;

    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenResponse {

        /**
         * Токен доступа.
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * Токен обновления.
         */
        @JsonProperty("refresh_token")
        private String refreshToken;

        /**
         * Тип токена.
         */
        @JsonProperty("token_type")
        private String tokenType;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        private String code;
        private String newPassword;

        // Геттеры и сеттеры
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationRequest {

        /**
         * Электронная почта пользователя.
         */
        @Schema(example = "Vlad72229@yandex.ru")
        private String email;

        /**
         * Пароль пользователя.
         */
        @Schema(example = "Vlad!123")
        private String password;
    }
}
