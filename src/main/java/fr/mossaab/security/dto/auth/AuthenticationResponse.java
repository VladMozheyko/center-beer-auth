package fr.mossaab.security.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    @NotNull
    @Schema(description = "Уникальный идентификатор пользователя в системе")
    private Long id;

    @NotNull
    @Schema(description = "Электронная почта пользователя (основной идентификатор для входа)")
    private String email;

    @NotEmpty
    @Schema(description = "Список ролей пользователя, определяющих его права доступа")
    private List<String> roles;

    @JsonProperty("access_token")
    @NotNull
    @Schema(description = "Токен доступа (JWT) для авторизации запросов")
    private String accessToken;

    @JsonProperty("refresh_token")
    @NotNull
    @Schema(description = "Токен обновления для получения нового access token")
    private String refreshToken;

    @JsonProperty("token_type")
    @NotNull
    @Schema(description = "Тип токена (обычно 'Bearer')", example = "Bearer")
    private String tokenType;

    @Schema(description = "Значение JWT‑cookie для автоматической авторизации")
    private String jwtCookie;

    @Schema(description = "Значение cookie с refresh token")
    private String refreshTokenCookie;

    @Schema(description = "ID устройства, с которого выполнен вход. Используется для привязки сессии")
    private String deviceId;
}
