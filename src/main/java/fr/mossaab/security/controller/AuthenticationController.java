package fr.mossaab.security.controller;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.payload.request.AuthenticationRequest;
import fr.mossaab.security.payload.request.RefreshTokenRequest;
import fr.mossaab.security.payload.request.RegisterRequest;
import fr.mossaab.security.payload.response.AuthenticationResponse;
import fr.mossaab.security.payload.response.RefreshTokenResponse;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.AuthenticationService;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Tag(name = "Authentication", description = "The Authentication API. Contains operations like login, logout, refresh-token etc.")
@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements() /*
This API won't have any security requirements. Therefore, we need to override the default security requirement configuration
with @SecurityRequirements()
*/
@RequiredArgsConstructor
public class AuthenticationController {
    public static final Pattern VALID_PASSWORD_REGEX =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*])(?=\\S+$).{8,20}$");
    public static final Pattern VALID_PHONE_NUMBER_REGEX =
            Pattern.compile("^\\+?[78][-\\(]?\\d{3}\\)?-?\\d{3}-?\\d{2}-?\\d{2}$", Pattern.CASE_INSENSITIVE);
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileDataRepository fileDataRepository;
    private boolean isValidPassword(String password) {
        Matcher matcher = VALID_PASSWORD_REGEX.matcher(password);
        return matcher.matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        Matcher matcher = VALID_PHONE_NUMBER_REGEX.matcher(phoneNumber);
        return matcher.matches();
    }

    @PostMapping(value = "/register", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> register(@RequestPart RegisterRequest request,@RequestPart(required = false) MultipartFile image) throws IOException {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        response.put("status", "success"); //
        response.put("notify", "Вы успешно зарегистрировались");
        response.put("answer", "registration success"); //
        errors.put("firstname", "");
        errors.put("lastname", "");
        errors.put("email", "");
        errors.put("password", "");
        errors.put("phoneNumber", "");
        errors.put("workerRole", "");
        errors.put("dateOfBirth", "");
        errors.put("photo", "");
        if (request.getFirstname() == null) {
            errors.put("firstname", "Неправильное имя пользователя");
        }
        if (request.getLastname() == null) {
            errors.put("lastname", "Неправильная фамилия");
        }
        if (request.getEmail() == null || userRepository.findByEmail(request.getEmail()).isPresent()) {
            errors.put("email", "Неверный почтовый ящик");
        }
        if (request.getPassword() == null || !isValidPassword(request.getPassword())) {
            errors.put("password", "Неверный пароль");
        }
        if (request.getPhoneNumber() == null || !isValidPhoneNumber(request.getPhoneNumber())) {
            errors.put("phoneNumber", "Неправильный номер телефона");
        }
        if (request.getWorkerRole() == null) {
            errors.put("workerRole", "Неверная роль");
        }
        if (request.getDateOfBirth() == null) {
            errors.put("dateOfBirth", "Неверная дата рождения");
        }
        int count = 0;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Проверяем, что ключ не пустой и значение пустое
            if (!key.isEmpty() && value.isEmpty()) {
                count++;
            }
        }
        if (count == 8) {
            response.put("errors", errors);
                /*User user = userService.register(userDto);
                FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image,user);
                fileDataRepository.save(uploadImage);
                 */
            AuthenticationResponse authenticationResponse = authenticationService.register(request);
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image,user);
            fileDataRepository.save(uploadImage);
            ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
            response.put("jwtCookie", jwtCookie.toString());
            ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
            response.put("refreshTokenCookie", refreshTokenCookie.toString());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        /*AuthenticationResponse authenticationResponse = authenticationService.register(request);
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString())
                .body(authenticationResponse);*/
        response.put("status", "error");
        response.put("notify", "Неверные данные");
        response.put("answer", "registration error");
        response.put("errors", errors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/authenticate")
    @Operation(
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Unauthorized",
                            responseCode = "401",
                            content = {@Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = "application/json")}
                    )
            }
    )
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authenticationResponse = authenticationService.authenticate(request);
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(authenticationResponse.getAccessToken());
        ResponseCookie refreshTokenCookie = refreshTokenService.generateRefreshTokenCookie(authenticationResponse.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString())
                .body(authenticationResponse);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.generateNewToken(request));
    }

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
    @GetMapping("/info")
    public Authentication getAuthentication(@RequestBody AuthenticationRequest request){
        return     authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
    }

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
}
