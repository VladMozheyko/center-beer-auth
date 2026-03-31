package fr.mossaab.security.controller;

import fr.mossaab.security.dto.social.SocialExchangeRequest;
import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.OAuthRequestStatus;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.exception.SocialAuthException;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.social.OAuth2Service;
import fr.mossaab.security.service.social.OneTimeAuthCodeService;
import fr.mossaab.security.service.social.SocialUserFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2/social")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "Вход/регистрация/линк через соцсети")
public class OAuth2Controller {

    private final OneTimeAuthCodeService oneTimeAuthCodeService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2Service oAuth2Service;

    @Operation(summary = "Обработка редиректа после соцлогина.",
            description = """
                    После успешного входа (или ошибки) по OAuth2 backend делает перенаправление (redirect) на фронт.
                    В параметры query (?auth_status, ?auth_code, ?message) фреймворк подставляет результат.
                    
                                ### Со статусов аутентификации через социальную сеть
                    
                                | Статус         | Описание для пользователя                                      | Возможные действия               | Доп.поля ответа                |
                                |----------------|---------------------------------------------------------------|----------------------------------|-------------------------------|
                                | SOCIAL_FOUND   | Вход выполнен.                                                | Перенаправление в профиль        | baseUserEmail, authCode       |
                                | NEW_SOCIAL_USER| Найдена ваша учётка. Привязка соцсети?                        | Показать модал "привязать"       | baseUserEmail, authCode       |
                                | EMAIL_LINKED   | Email соцсети зарегистрирован в системе (логин/пароль).       | Предложить войти по email/паролю | baseUserEmail                |
                                | NEW_ACCOUNT    | Такой e-mail не найден. Регистрация через соцсеть возможна.    | Показать регистрацию             | authCode, socialUser          |
                                | ERROR          | Ошибка или неоднозначная ситуация                              | Сообщить об ошибке               | message                       |
                    
                                В каждом ответе есть: \s
                                - message: подробное сообщение для UI
                                - baseUserEmail: email уже существующей учётки, если есть
                                - authCode: одноразовый код для продолжения регистрации/привязки
                                - socialUser: информация о профиле соцсети (для NEW_ACCOUNT)
                    
                    SPA обязан забрать параметры из URL.
                    
                    Пример:
                           `http://localhost:8081/?auth_status=social_found&auth_code=XXX&message=...`
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "302",
                            description = "Редирект на SPA с query-параметрами"
                    )
            }
    )
    @GetMapping("/collback")
    public void oauth2callback() {/* просто для автодокументации */}


//    @PostMapping("/exchange")
//    public ResponseEntity<?> exchange(
//            @RequestBody SocialExchangeRequest req,
//            @AuthenticationPrincipal UserDetails currentUser // null если не логинен
//    ) {
//
//        String authCode = req.getAuthCode();
//        OAuthRequestStatus frontendAction = req.getAction(); // ENUM или строка: REGISTER, LOGIN, LINK
//
//        // Извлечь socialUserInfo по code (и удалить code!)
//        SocialUserInfo info = oneTimeAuthCodeService.consumeCode(authCode);
//        if (info == null) throw new SocialAuthException("auth_code expired or invalid", HttpStatus.BAD_REQUEST.value());
//
//        OAuthProvider provider = req.getProvider(); // можно получить из info, если надо
//
//        // Универсальная логика
//        User user = null ;
//        switch (frontendAction) {
//            case LOGIN:
//                user = userRepository.findBySocialId(info.getId(), provider).orElse(null);
//                if (user == null) throw new SocialAuthException("User not found", HttpStatus.NOT_FOUND.value());
//                return buildTokenResponse(user, "Вход через соцсеть как существующий пользователь");
//            case REGISTER:
//                user = userRepository.findByEmail(info.getEmail()).orElse(null);
//                if (user != null)
//                    throw new SocialAuthException("Email уже есть, нельзя регистрация", HttpStatus.CONFLICT.value());
//                // Создать нового пользователя с socialId, provider, email и т.д. (используй свой сервис)
//                User newUser = oAuth2Service.createAccount(provider, info);
//                return buildTokenResponse(newUser, "Пользователь успешно зарегистрирован через соцсеть");
//            case LINK:
//                if (currentUser == null)
//                    throw new SocialAuthException("Необходимо войти под основной учёткой", HttpStatus.UNAUTHORIZED.value());
//                User existing = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
//                if (existing == null)
//                    throw new SocialAuthException("Основной пользователь не найден", HttpStatus.FORBIDDEN.value());
//                oAuth2Service.addAccount(existing, info, provider);
//                return buildTokenResponse(existing, "Успешная привязка");
//            default:
//                throw new SocialAuthException("Непонятный статус действия.",HttpStatus.NOT_IMPLEMENTED.value());
//        }
//    }

    private ResponseEntity<?> buildTokenResponse(User user, String message) {
        String jwt = jwtService.generateToken(user);
        String refresh = refreshTokenService.createRefreshToken(user.getId()).getToken();

        // Создание тела ответа с токенами
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("accessToken", jwt);
        responseBody.put("refreshToken", refresh);
        responseBody.put("email", user.getEmail());
        responseBody.put("message", message);
        responseBody.put("status", HttpStatusCode.valueOf(200).toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.generateJwtCookie(jwt).toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenService.generateRefreshTokenCookie(refresh).toString())
                .body(responseBody);
    }

    @PostMapping("/login")
    public ResponseEntity<?> socialLogin(@RequestBody SocialExchangeRequest req) {
        String authCode = req.getAuthCode();
        OAuthRequestStatus frontendAction = req.getAction(); // ENUM или строка: REGISTER, LOGIN, LINK

        // Извлечь socialUserInfo по code (и удалить code!)
        SocialUserInfo info = oneTimeAuthCodeService.consumeCode(authCode);
        if (info == null) throw new SocialAuthException("auth_code expired or invalid", HttpStatus.BAD_REQUEST.value());

        OAuthProvider provider = req.getProvider(); // можно получить из info, если надо

        User user;
        if (frontendAction.equals(OAuthRequestStatus.LOGIN)) {
            user = userRepository.findBySocialId(info.getId(), provider).orElse(null);
            if (user == null) throw new SocialAuthException("User not found", HttpStatus.NOT_FOUND.value());
            return buildTokenResponse(user, "Вход через соцсеть как существующий пользователь");
        } else {
            throw new SocialAuthException("Invalid request action", HttpStatus.BAD_REQUEST.value());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> socialRegister(@RequestBody SocialExchangeRequest req) {
        String authCode = req.getAuthCode();
        OAuthRequestStatus frontendAction = req.getAction(); // ENUM или строка: REGISTER, LOGIN, LINK

        // Извлечь socialUserInfo по code (и удалить code!)
        SocialUserInfo info = oneTimeAuthCodeService.consumeCode(authCode);
        if (info == null) throw new SocialAuthException("auth_code expired or invalid", HttpStatus.BAD_REQUEST.value());

        OAuthProvider provider = req.getProvider(); // можно получить из info, если надо

        User user;
        if (frontendAction.equals(OAuthRequestStatus.REGISTER)) {
            user = userRepository.findByEmail(info.getEmail()).orElse(null);
            if (user != null)
                throw new SocialAuthException("Email уже есть, нельзя регистрация", HttpStatus.CONFLICT.value());
            // Создать нового пользователя с socialId, provider, email и т.д. (используй свой сервис)
            User newUser = oAuth2Service.createAccount(provider, info);
            return buildTokenResponse(newUser, "Пользователь успешно зарегистрирован через соцсеть");
        } else {
            throw new SocialAuthException("Invalid request action", HttpStatus.BAD_REQUEST.value());
        }
    }

    @PostMapping("/link")
    public ResponseEntity<?> socialLink(@RequestBody SocialExchangeRequest req,
                                        @AuthenticationPrincipal UserDetails currentUser) {
        String authCode = req.getAuthCode();
        OAuthRequestStatus frontendAction = req.getAction(); // ENUM или строка: REGISTER, LOGIN, LINK

        // Извлечь socialUserInfo по code (и удалить code!)
        SocialUserInfo info = oneTimeAuthCodeService.consumeCode(authCode);
        if (info == null) throw new SocialAuthException("auth_code expired or invalid", HttpStatus.BAD_REQUEST.value());

        OAuthProvider provider = req.getProvider(); // можно получить из info, если надо

        if (frontendAction.equals(OAuthRequestStatus.LINK)) {
            if (currentUser == null)
                throw new SocialAuthException("Необходимо войти под основной учёткой", HttpStatus.UNAUTHORIZED.value());
            User existing = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
            if (existing == null)
                throw new SocialAuthException("Основной пользователь не найден", HttpStatus.FORBIDDEN.value());
            oAuth2Service.addAccount(existing, info, provider);
            return buildTokenResponse(existing, "Успешная привязка");
        } else {
            throw new SocialAuthException("Invalid request action", HttpStatus.BAD_REQUEST.value());
        }
    }
}