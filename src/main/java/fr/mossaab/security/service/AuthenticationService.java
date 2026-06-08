package fr.mossaab.security.service;


import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mossaab.security.dto.SessionInfoResponse;
import fr.mossaab.security.dto.auth.AuthenticationRequest;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.RegisterRequest;
import fr.mossaab.security.dto.auth.ResetPasswordRequest;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.validation.annotation.ValidRefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
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

    @Value("${app.server.public-url:https://api.center.beer/auth_service}")
    private String publicUrl;

    public AuthenticationResponse register(RegisterRequest request, String deviceInfo)  {
        log.info("[BASE REGISTER] - Процесс регистрации через логин и пароль, email:{}", request.getEmail());

        // Проверка существования пользователя с таким же email и activationCode == null
        var existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent() && existingUserByEmail.get().getActivationCode() == null) {
            log.warn("[BASE REGISTER] - Данный Email {} занят другим пользователем ", request.getEmail());
            throw new DuplicateResourceException(
                    "Пользователь с таким email уже существует и активирован.",
                    "email_exists"
            );
        }

        var existingUserByNickname = userRepository.findByNickname(request.getNickname());
        if (existingUserByNickname.isPresent() && existingUserByNickname.get().getActivationCode() == null) {
            log.warn("[BASE REGISTER] - Пользователь с таким никнеймом уже существует и активирован nickname={}",
                    request.getNickname());
            throw new DuplicateResourceException(
                    "Пользователь с таким никнеймом уже существует и активирован.",
                    "nickname_exists"
            );
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

        if (user.getEmail() != null && !user.getEmail().isEmpty() && !user.getEmail().isBlank()) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в CENTER.BEER. Ваша ссылка для активации: "+publicUrl+"/authentication/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Ссылка активации CENTER.BEER", message);
        }

        try {
            user = userRepository.save(user);

            // 1. для НОВОГО пользователя всегда генерируем новый deviceId
            String deviceId = UUID.randomUUID().toString();

            // 2. создаём refresh-токен для этого deviceId
            RefreshToken refreshToken =
                    refreshTokenService.createOrReuseRefreshToken(user.getId(), deviceId, deviceInfo);

            // 3. генерируем access-токен с deviceId в claim
            String jwt = jwtService.generateToken(user, deviceId);

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
                    .deviceId(deviceId)
                    .build();
        } catch (Exception e) {
            log.error("[BASE REGISTER] - Ошибка при сохранении пользователя {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void requestPasswordReset(String email) {
        log.info("[RESET PASSWORD] - Процесс сброса пароля");
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
        log.info("[RESET PASSWORD] - код сброшен и отправлен новый на {}", user.getEmail());
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
        log.info("[RESET PASSWORD] - процесс смены пароля");
        // 1. Совпадают ли пароли?
        if (!req.getNewPassword().equals(req.getNewPasswordRepeat())) {
            log.warn("[RESET PASSWORD] - пароли не совпадают");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Пароли не совпадают");
        }

        // 2. Есть ли пользователь с таким кодом?
        User user = userRepository.findByActivationCode(req.getCode())
                .orElse(null);
        if (user == null) {
            log.warn("[RESET PASSWORD] - Неверный или просроченный код");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Неверный или просроченный код");
        }

        // 3. Меняем пароль
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setActivationCode(null);          // обнуляем, чтобы 1 раз = 1 сброс
        userRepository.save(user);
        log.info("[RESET PASSWORD] - пароль успешно изменен для пользователя id: {}", user.getId());
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

    public void  resendActivationCode(String email) {
        log.info("[ACCOUNT ACTIVATION CODE] - отправка кода");
        User userOptional = userRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException(
                        "Пользователь с email %s не найден".formatted(email)));
        String activationCode = UUID.randomUUID().toString();
        userOptional.setActivationCode(activationCode);
        if (userOptional.getEmail() != null && !userOptional.getEmail().isEmpty() && !userOptional.getEmail().isBlank()) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в CENTER.BEER. Ваш ссылка активации: " + publicUrl +"/authentication/activate/%s",
                    userOptional.getUsername(),
                    userOptional.getActivationCode()
            );

            mailSender.send(userOptional.getEmail(), "Ссылка активации CENTER.BEER", message);
        }
        userRepository.save(userOptional);
        log.info("[ACCOUNT ACTIVATION CODE] - код сохранен и отправлен");
    }

    /**
     * Аутентифицирует пользователя.
     *
     * @param request Запрос на аутентификацию.
     * @return Ответ с данными пользователя и токенами.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request, String deviceInfo) {
        log.info("[AUTHENTICATION] - Аутентификация пользователя");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        logger.debug("[AUTHENTICATION] - Пользователь аутентифицирован!: {}, Role: {}",
                user.getEmail(), user.getRole().name());

        if (user.getActivationCode() != null) {
            log.warn("[AUTHENTICATION] - email не подтвержден");
            throw new IllegalStateException("EMAIL_NOT_CONFIRMED");
        }

        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();

        String deviceIdFromClient = request.getDeviceId(); // может быть null/пустой

        // создаём/переиспользуем refresh-токен
        RefreshToken refreshToken = refreshTokenService.createOrReuseRefreshToken(
                user.getId(),
                deviceIdFromClient,
                deviceInfo
        );

        String deviceId = refreshToken.getDeviceId();

        //генерируем JWT c deviceId в claim
        var jwt = jwtService.generateToken(user, deviceId);

        ResponseCookie jwtCookie = jwtService.generateJwtCookie(jwt);
        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(refreshToken.getToken());

        log.info("[AUTHENTICATION] - успешная аутентификация для email:{}", user.getEmail());

        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .roles(roles)
                .email(user.getEmail())
                .id(user.getId())
                .refreshToken(refreshToken.getToken())
                .tokenType(TokenType.BEARER.name())
                .jwtCookie(jwtCookie.toString())
                .refreshTokenCookie(refreshTokenCookie.toString())
                .deviceId(deviceId)
                .build();
    }

    @Transactional
    public ResponseEntity<Void> logoutAllDevices(HttpServletRequest request, boolean isExitThisDevice) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // удалить все refresh-токены пользователя
        if(isExitThisDevice) {
            refreshTokenService.deleteAllByUserId(user.getId());
            ResponseCookie cleanRefreshTokenCookie = refreshTokenService.getCleanRefreshTokenCookie();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cleanRefreshTokenCookie.toString())
                    .build();
        }

        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);

        refreshTokenService.deleteEverythingExceptTheCurrentDevice(refreshToken, user.getId());
        return ResponseEntity.ok().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<List<SessionInfoResponse>> getActiveSessions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<RefreshToken> tokens = refreshTokenService.getAllByUserId(user.getId());

        List<SessionInfoResponse> response = tokens.stream()
                .map(rt -> SessionInfoResponse.builder()
                        .id(rt.getId())
                        .token(rt.getToken())
                        .expiryDate(rt.getExpiryDate())
                        .revoked(rt.isRevoked())
                        .createdAt(rt.getCreatedAt())
                        .lastUsedAt(rt.getLastUsedAt())
                        .deviceInfo(rt.getDeviceInfo())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
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
        @ValidRefreshToken
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