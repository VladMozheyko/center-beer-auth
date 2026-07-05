package fr.mossaab.security.builder;

import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.AuthenticationResponseDto;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.TokenType;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.UserIpTempService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Класс для сборки ответов аутентификации и регистрации пользователей.
 * Обрабатывает создание токенов, cookie и сохранение IP-адресов.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationResponseBuilder {

    private final UserIpTempService userIpTempService;
    /**
     * -- GETTER --
     *  Возвращает инстанс IpHelper для получения IP-адресов.
     *  Используется сервисом Authentication для сохранения IP при регистрации.
     */
    @Getter
    private final IpHelper ipHelper;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    /**
     * Строит полный ответ аутентификации для пользователя.
     * Сохраняет IP-адрес клиента, создает refresh-токен и генерирует access-токен с deviceId.
     * 
     * @param user пользователь
     * @param deviceId идентификатор устройства
     * @param request HTTP-запрос (для получения IP и User-Agent)
     * @return AuthenticationResponse с токенами и списком IP
     */
    public AuthenticationResponse buildAuthenticationResponse(User user, String deviceId, HttpServletRequest request) {
        // Сохраняем IP-адрес клиента
        String ip = ipHelper.getClientIp(request);
        log.info("Получен IP-адрес для пользователя {}: {}", user.getId(), ip);
        
        userIpTempService.saveIpTemp(user.getId(), ip);
        List<UserIpTempDto> ips = userIpTempService.getTrackedIpForUser(user.getId());
        log.debug("После сохранения IP получено {} записей для пользователя {}", ips.size(), user.getId());

        // Получаем информацию об устройстве
        String deviceInfo = request.getHeader("User-Agent");

        // Создаем refresh-токен
        RefreshToken refreshToken = refreshTokenService.createOrReuseRefreshToken(
                user.getId(),
                deviceId,
                deviceInfo
        );
        deviceId = refreshToken.getDeviceId();

        // Генерируем access-токен
        String jwt = jwtService.generateToken(user, deviceId);

        // Получаем роли пользователя
        List<String> roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();

        return AuthenticationResponse.builder()
                .jwtCookie(jwtService.generateJwtCookie(jwt))
                .refreshTokenCookie(refreshTokenService.generateRefreshTokenCookie(jwt))
                .accessToken(jwt)
                .email(user.getEmail())
                .id(user.getId())
                .refreshToken(refreshToken.getToken())
                .roles(roles)
                .tokenType(TokenType.BEARER.name())
                .deviceId(deviceId)
                .userIPs(ips)
                .build();
    }

    /**
     * Создает объект ответа с токенами и устанавливает cookie-файлы JWT и refresh token.
     * Добавляет в ответ список IP-адресов пользователя и статус операции.
     * 
     * @param response объект AuthenticationResponse
     * @param message сообщение об операции
     * @return ResponseEntity с токенами и cookie
     */
    public ResponseEntity<AuthenticationResponseDto> buildResponseWithCookies(AuthenticationResponse response, String message) {
        AuthenticationResponseDto dto = AuthenticationResponseDto.builder()
                .email(response.getEmail())
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .message(message)
                .deviceId(response.getDeviceId())
                .status(String.valueOf(HttpStatus.OK.value()))
                .lastIpAddress(response.getUserIPs())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.getJwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, response.getRefreshTokenCookie().toString())
                .body(dto);
    }

    /**
     * Строит стандартный ответ с токенами для endpoint'ов аутентификации (для OAuth2-провайдеров).
     * Создает AuthenticationResponse и устанавливает cookie с токенами.
     *
     * @param user пользователь
     * @param message сообщение об операции
     * @param request HTTP-запрос
     * @param deviceId идентификатор устройства
     * @return ResponseEntity с токенами и cookie
     */
    public ResponseEntity<AuthenticationResponseDto> buildResponseWithCookies(
            User user,
            String message,
            HttpServletRequest request,
            String deviceId
    ) {
        var response = buildAuthenticationResponse(user, deviceId, request);
        return buildResponseWithCookies(response, message);
    }
}
