package fr.mossaab.security.dto.auth;

import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.entities.UserIpTemp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ аутентификации (логин/регистрация/соц. вход)")
public class AuthenticationResponseDto {

    @Schema(description = "JWT access токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh токен для обновления access токена", example = "d0f8d9c3-3b4a-4c89-9a3a-1a2b3c4d5e6f")
    private String refreshToken;

    @Schema(description = "Идентификатор устройства, к которому привязаны токены", example = "device-8f7a1b2c-3d4e-5f6a-7b8c-9d0e1f2a3b4c")
    private String deviceId;

    @Schema(description = "Служебное сообщение от сервера", example = "Успешный вход")
    private String message;

    @Schema(description = "Email аутентифицированного пользователя", example = "user@example.com")
    private String email;

    @Schema(description = "Статус операции", example = "SUCCESS")
    private String status;

    @Schema(description = "Список IP адресов используемых пользователем за последние 5 минут с временем использования")
    private List<UserIpTempDto> lastIpAddress;
}
