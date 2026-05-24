package fr.mossaab.security.dto.auth;

import fr.mossaab.security.validation.annotation.ValidEmail;
import fr.mossaab.security.validation.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    @Schema(description = "Электронная почта пользователя", example = "Vlad72229@yandex.ru")
    @ValidEmail
    private String email;

    @Schema(description = "Пароль пользователя", example = "Vlad!123")
    @ValidPassword
    private String password;

    @Schema(
            example = "9e0f1139-52e1-4e6d-81fc-1999d7c27bf6",
            description = """
                    ID устройства для его идентификации в последующих запросах.
                    При первом запросе может быть пустым — сервис сгенерирует ID и вернёт в ответе."""
    )
    private String deviceId;

}
