package fr.mossaab.security.controller;

import fr.mossaab.security.dto.auth.*;
import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.service.AuthenticationService;
import fr.mossaab.security.service.PhoneRegistrationFacade;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.StorageService;
import fr.mossaab.security.validation.annotation.ValidPdfFileName;
import fr.mossaab.security.validation.annotation.ValidSmsCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;

@Tag(name = "Аутентификация", description = "API для работы с аутентификацией пользователей")
@RestController
@RequestMapping("/authentication")
@SecurityRequirements()
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;
    private final StorageService storageService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneRegistrationFacade phoneRegistrationFacade;
    private final UserRepository userRepository;

    @Operation(summary = "Получить пользователя по идентификатору")
    @GetMapping("/by-id/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable
            @Min(value = 1, message = "ID пользователя должен быть положительным числом")
            Long id
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + id + " не найден"));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .country(user.getLocation() != null ? user.getLocation().getCountry() : null)
                .city(user.getLocation() != null ? user.getLocation().getCity() : null)
                .latitude(user.getLocation() != null ? user.getLocation().getLatitude() : null)
                .longitude(user.getLocation() != null ? user.getLocation().getLongitude() : null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-phone")
    public ResponseEntity<?> registerByPhone(@Valid @RequestBody PhoneRegisterRequest dto) {
        phoneRegistrationFacade.start(dto);
        return ResponseEntity.ok("Код отправлен");
    }


    @PostMapping("/confirm-phone")
    public ResponseEntity<?> confirmPhone(@Valid @RequestBody ConfirmPhoneRequest req) {
        phoneRegistrationFacade.confirm(req.getPhone(), req.getCode());
        return ResponseEntity.ok("Телефон подтверждён");
    }

    @Operation(summary = "Загрузка PDF-файла из файловой системы", description = "Этот endpoint позволяет загрузить PDF-файл из файловой системы.")
    @GetMapping("/file-system-pdf/{fileName}")
    @Validated
    public ResponseEntity<?> downloadPdfFromFileSystem(
            @ValidPdfFileName @PathVariable String fileName
    ) throws IOException {
        byte[] pdfData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("application/pdf"))
                .body(pdfData);
    }

    @Operation(summary = "Регистрация пользователя", description = "Позволяет новому пользователю зарегистрироваться в системе.")
    @PostMapping(value = "/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        String deviceInfo = httpServletRequest.getHeader("User-Agent");
        authenticationService.register(request, deviceInfo);
        return ResponseEntity.ok().body("Код активации для активации аккаунта успешно отправлен на почтовый адрес");
    }


    @Operation(summary = "Активация пользователя", description = "Позволяет отправить код активации для регистрации.")
    @GetMapping("/activate/{code}")
    public ResponseEntity<Object> activateUser(
            @ValidSmsCode @PathVariable String code
    ) {
        authenticationService.activateUser(String.valueOf(code));
        return new ResponseEntity<>("Пользователь успешно зарегистрирован", HttpStatus.OK);
    }

    @Operation(summary = "Вход пользователя 🚨(modify)", description = "Этот endpoint позволяет пользователю войти в систему.")
    @PostMapping("/login")
    public ResponseEntity<Object> authenticate(@Valid @RequestBody AuthenticationRequest request, HttpServletRequest httpServletRequest) {
        String deviceInfo = httpServletRequest.getHeader("User-Agent");
        AuthenticationResponse authenticationResponse = authenticationService.authenticate(request, deviceInfo);

        // Создание тела ответа с токенами
        AuthenticationResponseDto dto = AuthenticationResponseDto.builder()
                .accessToken(authenticationResponse.getAccessToken())
                .refreshToken(authenticationResponse.getRefreshToken())
                .message("Вход в систему пользователя успешно совершен")
                .deviceId(authenticationResponse.getDeviceId())
                .status(String.valueOf(HttpStatus.OK.value()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.getJwtCookie())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.getRefreshTokenCookie())
                .body(dto);
    }


    @Operation(summary = "Отправить повторный код активации", description = "Этот endpoint позволяет отправить повторный код активации пользователю.")
    @PostMapping("/resend-activation-code")
    public ResponseEntity<Object> resendActivationCode(
            @Valid @RequestBody EmailRequest request
    ) throws ParseException {
        authenticationService.resendActivationCode(request.getEmail());
        return new ResponseEntity<>("Код подтверждения аккаунта успешно отправлен на почту", HttpStatus.OK);
    }


    @Operation(summary = "Обновление токена", description = "Этот endpoint позволяет обновить токен.")
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationService.RefreshTokenResponse> refreshToken(@Valid @RequestBody AuthenticationService.RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.generateNewToken(request));
    }

    @Operation(summary = "Обновление токена через куки", description = "Этот endpoint позволяет обновить токен с использованием куки.")
    @PostMapping("/refresh-token-cookie")
    public ResponseEntity<Void> refreshTokenCookie(HttpServletRequest request) {
        return authenticationService.refreshTokenUsingCookie(request);
    }


    @Operation(summary = "Выход из системы 🚨(deprecated)", description = "УСТАРЕЛ! Этот endpoint позволяет пользователю выйти из системы.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        return authenticationService.logout(request);
    }


    @Operation(summary = "Запрос на смену пароля", description = "Этот endpoint отправляет код активации на почту пользователя.")
    @PostMapping("/request-password-reset")
    public ResponseEntity<Object> requestPasswordReset(@Valid @RequestBody EmailRequest emailRequest) {
        authenticationService.requestPasswordReset(emailRequest.getEmail());
        return new ResponseEntity<>("Код для смены пароля успешно отправлен", HttpStatus.OK);
    }

    @Operation(summary = "Смена пароля по коду")
    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authenticationService.resetPassword(request);
    }
}
