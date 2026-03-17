package fr.mossaab.security.service;


import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.RegisterRequest;
import fr.mossaab.security.dto.auth.ResetPasswordRequest;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final MailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    @Value("${app.server.base-url}")
    private String baseUrl;
    @Value("${app.server.public-url:https://api.center.beer/auth_service}")
    private String publicUrl;
    public AuthenticationResponse register(RegisterRequest request)  {
        // Проверка существования пользователя с таким же email и activationCode == null
        var existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent() && existingUserByEmail.get().getActivationCode() == null) {
            throw new DuplicateResourceException("Пользователь с таким email уже существует и активирован.", "email_exists");
        }
        var existingUserByNickname = userRepository.findByNickname(request.getNickname());
        if (existingUserByNickname.isPresent() && existingUserByNickname.get().getActivationCode() == null) {
            throw new DuplicateResourceException("Пользователь с таким никнеймом уже существует и активирован.", "nickname_exists");
        }

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .temporarySecondsBalance(0)
                .tempEmail(null)
                .nickname(request.getNickname())
                .createdAt(LocalDateTime.now())
                .build();
        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);
        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в CENTER.BEER. Ваша ссылка для активации: "+publicUrl+"/authentication/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Ссылка активации CENTER.BEER", message);
        }
        user = userRepository.save(user);
        var jwt = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь с email %s не найден".formatted(email)));

        /* ---------- 1. генерируем 4-значный код ---------- */
        String resetCode = String.format("%04d", new SecureRandom().nextInt(10_000));

        /* ---------- 2. сохраняем код в activationCode ---------- */
        user.setActivationCode(resetCode);
        userRepository.save(user);

        /* ---------- 3. формируем письмо БЕЗ ссылки ---------- */
        String message = """
                Здравствуйте, %s!

                Ваш код для смены пароля в CENTER.BEER:

                %s

                Введите его в приложении/на сайте. 
                Если вы не запрашивали смену пароля, просто проигнорируйте это письмо.
                """.formatted(user.getUsername(), resetCode);

        mailSender.send(user.getEmail(), "Код для смены пароля", message);
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
    public ResponseEntity<Object> resetPassword(ResetPasswordRequest req) {

        // 1. Совпадают ли пароли?
        if (!req.getNewPassword().equals(req.getNewPasswordRepeat())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Пароли не совпадают");
        }

        // 2. Есть ли пользователь с таким кодом?
        User user = userRepository.findByActivationCode(req.getCode())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Неверный или просроченный код");
        }

        // 3. Меняем пароль
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setActivationCode(null);          // обнуляем, чтобы 1 раз = 1 сброс
        userRepository.save(user);

        return ResponseEntity.ok("Пароль успешно изменён");
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
                            "Добро пожаловать в CENTER.BEER. Ваш ссылка активации: " + publicUrl +"/authentication/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Ссылка активации CENTER.BEER", message);
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
        logger.debug("Step 1");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        logger.debug("Step 2");
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        logger.debug("User authenticated: {}, Role: {}", user.getEmail(), user.getRole().name());
        if (user.getActivationCode() != null) {
            throw new IllegalStateException("EMAIL_NOT_CONFIRMED");
        }
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
        User userEntity = userRepository.findByActivationCode(code)
                .orElse(null);
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


}
