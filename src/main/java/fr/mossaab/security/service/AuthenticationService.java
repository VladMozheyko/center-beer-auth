package fr.mossaab.security.service;


import fr.mossaab.security.dto.request.RefreshTokenRequest;
import fr.mossaab.security.dto.request.ResetPasswordRequest;
import fr.mossaab.security.dto.response.RefreshTokenResponse;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.enums.WorkerRole;
import fr.mossaab.security.dto.request.AuthenticationRequest;
import fr.mossaab.security.dto.request.RegisterRequest;
import fr.mossaab.security.dto.response.AuthenticationResponse;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

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
     * Генерирует активационный код для пользователя.
     *
     * @return Сгенерированный активационный код.
     */
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
                .workerRoles(WorkerRole.valueOf(request.getWorkerRole()))
                .phoneNumber(request.getPhoneNumber())
                .build();
        String activationCode = generateActivationCode();
        user.setActivationCode(activationCode);
        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в Kotitonttu. Ваш код активации: %s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Код активации Kotitonttu", message);
        }
        user = userRepository.save(user);
        var jwt = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image,"" ,user);
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
        String activationCode = generateActivationCode();
        user.setActivationCode(activationCode);
        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format(
                    "Здравствуйте, %s! \n" +
                            "Добро пожаловать в Kotitonttu. Ваш код активации: %s",
                    user.getUsername(),
                    user.getActivationCode()
            );

            mailSender.send(user.getEmail(), "Код активации Kotitonttu", message);
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

    /**
     * Активирует пользователя по активационному коду.
     *
     * @param code Активационный код пользователя.
     * @return true, если пользователь успешно активирован, иначе false.
     */
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
}
