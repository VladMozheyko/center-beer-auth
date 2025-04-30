package fr.mossaab.security.dto.auth;

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

    /**
     * Электронная почта пользователя.
     */
    @Schema(example = "Vlad72229@yandex.ru")
    private String email;

    /**
     * Пароль пользователя.
     */
    @Schema(example = "Vlad!123")
    private String password;
}
