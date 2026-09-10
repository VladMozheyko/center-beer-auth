package fr.mossaab.security.service;


import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mossaab.security.builder.AuthenticationResponseBuilder;
import fr.mossaab.security.dto.SessionInfoResponse;
import fr.mossaab.security.dto.auth.*;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.UserStatus;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.exception.BadRequestException;
import fr.mossaab.security.exception.TooManyRequestsException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationResponseBuilder responseBuilder;
    private final JwtService jwtService;
    private final UserIpTempService userIpTempService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final MailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    @Value("${app.server.base-url:https://api.center.beer/auth_service}")
    private String publicUrl;

    public void register(RegisterRequest request, HttpServletRequest httpServletRequest)  {
        log.info("[BASE REGISTER] - Процесс регистрации через логин и пароль, email:{}, nickname:{}", 
                request.getEmail(), request.getNickname());

        LocalDateTime now = LocalDateTime.now();

        // ========== 1. Проверка nickname ==========
        var existingUserByNickname = userRepository.findByNickname(request.getNickname());
        if (existingUserByNickname.isPresent()) {
            User nicknameUser = existingUserByNickname.get();
            if (nicknameUser.getStatus() == UserStatus.ACTIVATED) {
                // Никнейм занят активированным аккаунтом
                log.warn("[BASE REGISTER] - Никнейм {} занят активированным аккаунтом", request.getNickname());
                throw new DuplicateResourceException(
                        "Пользователь с таким никнеймом уже существует и активирован.",
                        "nickname_exists"
                );
            }
            // Никнейм занят неактивированным аккаунтом — проверяем таймаут
            if (nicknameUser.getLastCodeSentAt() != null) {
                long minutesSinceLastCode = ChronoUnit.MINUTES.between(nicknameUser.getLastCodeSentAt(), now);
                if (minutesSinceLastCode < 5) {
                    log.warn("[BASE REGISTER] - Повторная регистрация с никнеймом {} через {} мин (нужно 5)", 
                            request.getNickname(), minutesSinceLastCode);
                    throw new TooManyRequestsException(
                            "Подождите 5 минут перед повторной регистрацией с этим никнеймом"
                    );
                }
                // Прошло > 5 минут — разрешаем перезапись
                log.info("[BASE REGISTER] - Перезапись неактивированного аккаунта с никнеймом {} (прошло {} мин)", 
                        request.getNickname(), minutesSinceLastCode);
                
                // Обновляем email и генерируем новый код
                nicknameUser.setEmail(request.getEmail());
                nicknameUser.setActivationCode(UUID.randomUUID().toString());
                nicknameUser.setLastCodeSentAt(now);
                userRepository.save(nicknameUser);

                // Отправляем письмо на новый email
                sendActivationEmail(nicknameUser);

                String ip = responseBuilder.getIpHelper().getClientIp(httpServletRequest);
                userIpTempService.saveIpTemp(nicknameUser.getId(), ip);
                return;
            }
        }

        // ========== 2. Проверка email ==========
        var existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent()) {
            User emailUser = existingUserByEmail.get();
            if (emailUser.getStatus() == UserStatus.ACTIVATED) {
                log.warn("[BASE REGISTER] - Email {} занят активированным аккаунтом", request.getEmail());
                throw new DuplicateResourceException(
                        "Пользователь с таким email уже существует и активирован.",
                        "email_exists"
                );
            }
            // Email занят неактивированным — можно разрешить перезапись (опционально)
            // Сейчас просто продолжаем создание нового пользователя
            log.info("[BASE REGISTER] - Email {} занят неактивированным аккаунтом, создаём нового", request.getEmail());
        }

        // ========== 3. Создание нового пользователя ==========
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .temporarySecondsBalance(0)
                .tempEmail(null)
                .nickname(request.getNickname())
                .uuid(UUID.randomUUID().toString())
                .createdAt(now)
                .status(UserStatus.PENDING)
                .lastCodeSentAt(now)
                .build();

        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);

        sendActivationEmail(user);

        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            log.error("[BASE REGISTER] - Ошибка при сохранении пользователя {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        String ip = responseBuilder.getIpHelper().getClientIp(httpServletRequest);
        userIpTempService.saveIpTemp(user.getId(), ip);
    }

    /**
     * Отправляет письмо с кодом активации на указанный email.
     */
    private void sendActivationEmail(User user) {
        if (user.getEmail() != null && !user.getEmail().isEmpty() && !user.getEmail().isBlank()) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в CENTER.BEER. Ваша ссылка для активации: "+publicUrl+"/authentication/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );
            mailSender.send(user.getEmail(), "Ссылка активации CENTER.BEER", message);
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
    public AuthenticationResponse authenticate(AuthenticationRequest request, HttpServletRequest httpRequest) {
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

        String deviceIdFromClient = request.getDeviceId(); // может быть null/пустой

        var authResponse = responseBuilder.buildAuthenticationResponse(user, deviceIdFromClient, httpRequest);
        log.info("[AUTHENTICATION] - успешная аутентификация для email:{}", user.getEmail());

        return authResponse;
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
                .orElseThrow(() -> new BadRequestException("Неверный или устаревший код активации. Запросите новый"));
        
        if (userEntity.getStatus() != UserStatus.PENDING) {
            throw new BadRequestException("Аккаунт уже активирован");
        }
        
        if (!Objects.equals(code, userEntity.getActivationCode())) {
            throw new BadRequestException("Введенный код не совпадает с истинным");
        }
        
        userEntity.setStatus(UserStatus.ACTIVATED);
        userEntity.setActivationCode(null);
        userRepository.save(userEntity);
        return true;
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
