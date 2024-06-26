package fr.mossaab.security.controller;

import fr.mossaab.security.dto.response.*;
import fr.mossaab.security.entities.FileData;

import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import fr.mossaab.security.dto.request.*;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.api.AuthenticationService;
import fr.mossaab.security.service.api.JwtService;
import fr.mossaab.security.service.api.RefreshTokenService;
import fr.mossaab.security.service.impl.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Tag(name = "Authentication", description = "The Authentication API. Contains operations like login, logout, refresh-token etc.")
@RestController
@RequestMapping("/auth")
@SecurityRequirements()
@RequiredArgsConstructor
public class AuthController {
    public static final Pattern VALID_PASSWORD_REGEX =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*])(?=\\S+$).{8,20}$");
    public static final Pattern VALID_PHONE_NUMBER_REGEX =
            Pattern.compile("^\\+?[78][-\\(]?\\d{3}\\)?-?\\d{3}-?\\d{2}-?\\d{2}$", Pattern.CASE_INSENSITIVE);
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final FileDataRepository fileDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final MailSender mailSender;


    private boolean isValidPassword(String password) {
        Matcher matcher = VALID_PASSWORD_REGEX.matcher(password);
        return matcher.matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        Matcher matcher = VALID_PHONE_NUMBER_REGEX.matcher(phoneNumber);
        return matcher.matches();
    }
    @Operation(summary = "Регистрация пользователя", description = "Этот эндпоинт позволяет пользователю зарегистрироваться.")
    @PostMapping(value = "/register", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> register(@RequestPart RegisterRequest request, @RequestPart(required = false) MultipartFile image) throws IOException, ParseException {
        RegisterResponse response = new RegisterResponse();
        ErrorsRegisterDto errors = new ErrorsRegisterDto();

        response.setStatus("success");
        response.setNotify("Вы успешно зарегистрировались");
        response.setAnswer("registration success");

        errors.setFirstname("");
        errors.setLastname("");
        errors.setEmail("");
        errors.setPassword("");
        errors.setPhoneNumber("");
        errors.setWorkerRole("");
        errors.setDateOfBirth("");
        errors.setPhoto("");

        // Логика проверки и заполнения ошибок
        if (request.getFirstname() == null || request.getFirstname().isEmpty()) {
            errors.setFirstname("Неправильное имя пользователя");
        }
        if (request.getLastname() == null || request.getLastname().isEmpty()) {
            errors.setLastname("Неправильная фамилия");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty() || (userRepository.findByEmail(request.getEmail()).isPresent() && userRepository.findByEmail(request.getEmail()).get().getActivationCode() == null)) {
            errors.setEmail("Неверный почтовый ящик");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent() && userRepository.findByEmail(request.getEmail()).get().getActivationCode() != null){
            userRepository.delete(userRepository.findByEmail(request.getEmail()).get());
        }
        if (request.getPassword() == null || !isValidPassword(request.getPassword()) || request.getPassword().isEmpty()) {
            errors.setPassword("Неверный пароль");
        }
        if (request.getPhoneNumber() == null || !isValidPhoneNumber(request.getPhoneNumber()) || request.getPhoneNumber().isEmpty()) {
            errors.setPhoneNumber("Неправильный номер телефона");
        }
        if (request.getWorkerRole() == null || request.getWorkerRole().isEmpty()) {
            errors.setWorkerRole("Неверная роль");
        }
        if (request.getDateOfBirth() == null) {
            errors.setDateOfBirth("Неверная дата рождения");
        }

        int count = 0;
        for (Field field : errors.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.get(errors) != null && field.get(errors).toString().isEmpty()) {
                    count++;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (count == 8) {
            response.setErrors(errors);
            AuthenticationResponse authenticationResponse = authenticationService.register(request);
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image, user);
            fileDataRepository.save(uploadImage);
            ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
            ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(response);
        }

        response.setStatus("error");
        response.setNotify("Неверные данные");
        response.setAnswer("registration error");
        response.setErrors(errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @Operation(summary = "Активация пользователя", description = "Этот эндпоинт позволяет активировать пользователя.")
    @PostMapping("/activate")
    public ResponseEntity<Object> activateUser(@RequestBody Map<String, String> requestBody) {
        ActivateResponse response = new ActivateResponse();
        ErrorActivateDto errors = new ErrorActivateDto();

        response.setStatus("success");
        response.setNotify("Активация успешна");
        response.setAnswer("activate success");

        errors.setCode("");

        String code = requestBody.get("code");

        if (userRepository.findByActivationCode(code) == null) {
            errors.setCode("Неверный код");
        }
        int count = 0;
        if (errors.getCode().isEmpty()) {
            count++;
        }

        if (count == 1) {
            authenticationService.activateUser(code);
            response.setErrors(errors);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.setStatus("error");
        response.setNotify("Некорректные данные");
        response.setAnswer("activate error");
        response.setErrors(errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Вход пользователя", description = "Этот эндпоинт позволяет пользователю войти в систему.")
    @PostMapping("/login")
    public ResponseEntity<Object> authenticate(@RequestBody AuthenticationRequest request) {
        LoginResponse response = new LoginResponse();
        ErrorsLoginDto errors = new ErrorsLoginDto();

        response.setStatus("success");
        response.setNotify("Успешный вход в систему");
        response.setAnswer("login success");

        errors.setEmail("");
        errors.setPassword("");

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            errors.setEmail("Неверный почтовый ящик");
        } else {
            User user = userOptional.get();
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                errors.setPassword("Неверный пароль");
            }
        }

        int count = 0;
        for (Field field : errors.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.get(errors) != null && field.get(errors).toString().isEmpty()) {
                    count++;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (count == 2) {
            response.setErrors(errors);
            AuthenticationResponse authenticationResponse = authenticationService.authenticate(request);
            ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
            ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());

            response.setAccessToken(jwtCookie.getValue());
            response.setRefreshToken(refreshTokenCookie.getValue());
            response.setTokenType("Bearer");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(response);
        }

        response.setStatus("error");
        response.setNotify("Неверные данные");
        response.setAnswer("login error");
        response.setErrors(errors);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Отправить повторный код активации", description = "Этот эндпоинт позволяет отправить повторный код активации пользователю.")
    @PostMapping("/resend-activation-code")
    public ResponseEntity<Object> resendActivationCode(@RequestBody Map<String, String> requestBody) throws ParseException {
        ResendActivationCodeResponse response = new ResendActivationCodeResponse();
        ErrorResendActivationCodeDto errors = new ErrorResendActivationCodeDto();

        response.setStatus("success");
        response.setNotify("Код активации успешно отправлен");
        response.setAnswer("resend activation code success");

        errors.setEmail("");

        String email = requestBody.get("email");

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            errors.setEmail("Неверный почтовый ящик");
        }

        int count = 0;
        for (Field field : errors.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.get(errors) != null && field.get(errors).toString().isEmpty()) {
                    count++;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (count == 1) {
            User user = userOptional.get();
            authenticationService.resendActivationCode(user);
            response.setErrors(errors);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.setStatus("error");
        response.setNotify("Некорректные данные");
        response.setAnswer("resend activation code error");
        response.setErrors(errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "Обновление токена", description = "Этот эндпоинт позволяет обновить токен.")
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.generateNewToken(request));
    }
    @Operation(summary = "Обновление токена через куки", description = "Этот эндпоинт позволяет обновить токен с использованием куки.")
    @PostMapping("/refresh-token-cookie")
    public ResponseEntity<Void> refreshTokenCookie(HttpServletRequest request) {
        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);
        RefreshTokenResponse refreshTokenResponse = refreshTokenService
                .generateNewToken(new RefreshTokenRequest(refreshToken));
        ResponseCookie NewJwtCookie = jwtService.generateJwtCookie(refreshTokenResponse.getAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, NewJwtCookie.toString())
                .build();
    }
    @Operation(summary = "Получение аутентификации", description = "Этот эндпоинт позволяет получить аутентификацию.")
    @GetMapping("/info")
    public Authentication getAuthentication(@RequestBody AuthenticationRequest request){
        return     authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
    }
    @Operation(summary = "Выход из системы", description = "Этот эндпоинт позволяет пользователю выйти из системы.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request){
        String refreshToken = refreshTokenService.getRefreshTokenFromCookies(request);
        if(refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        ResponseCookie jwtCookie = jwtService.getCleanJwtCookie();
        ResponseCookie refreshTokenCookie = refreshTokenService.getCleanRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString())
                .build();

    }


    @Operation(summary = "Запрос на смену пароля", description = "Этот эндпоинт отправляет код активации на почту пользователя.")
    @PostMapping("/request-password-reset")
    public ResponseEntity<Object> requestPasswordReset(@RequestBody Map<String, String> requestBody) {
        PasswordResetRequestResponse response = new PasswordResetRequestResponse();
        ErrorPasswordResetRequestDto errors = new ErrorPasswordResetRequestDto();

        response.setStatus("success");
        response.setNotify("Код активации успешно отправлен на почту");
        response.setAnswer("password reset request success");

        errors.setEmail("");

        String email = requestBody.get("email");

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            errors.setEmail("Неверный почтовый ящик");
        } else {
            User user = userOptional.get();
            String activationCode = generateActivationCode(); // Генерация нового кода
            user.setActivationCode(activationCode);
            userRepository.save(user);
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Ваш код смены для смены пароля: %s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Код смены пароля в Kotitonttu", message);
        }

        if (!errors.getEmail().isEmpty()) {
            response.setStatus("error");
            response.setNotify("Некорректные данные");
            response.setAnswer("password reset request error");
            response.setErrors(errors);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Смена пароля", description = "Этот эндпоинт позволяет сменить пароль пользователя с использованием кода активации.")
    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(@RequestBody ResetPasswordRequest request) {
        PasswordResetResponse response = new PasswordResetResponse();
        ErrorPasswordResetDto errors = new ErrorPasswordResetDto();
        System.out.println("Код активации: " + request.getCode());
        System.out.println("Новый пароль: " + request.getNewPassword());
        response.setStatus("success");
        response.setNotify("Пароль успешно изменен");
        response.setAnswer("password reset success");

        errors.setCode("");
        errors.setPassword("");

        User user = userRepository.findByActivationCode(request.getCode());

        if (user == null) {
            errors.setCode("Неверный код активации");
        } else if (!isValidPassword(request.getNewPassword())) {
            errors.setPassword("Неверный пароль");
        }

        if (errors.getCode().isEmpty() && errors.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setActivationCode(null); // Удаление использованного кода
            userRepository.save(user);
        } else {
            response.setStatus("error");
            response.setNotify("Некорректные данные");
            response.setAnswer("password reset error");
            response.setErrors(errors);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String generateActivationCode() {
        int length = 4;
        String digits = "0123456789";
        Random random = new Random();

        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(digits.charAt(random.nextInt(digits.length())));
        }

        return code.toString();
    }
}
