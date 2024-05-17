package fr.mossaab.security.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mossaab.security.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ на аутентификацию пользователя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    /**
     * Уникальный идентификатор пользователя.
     */
    private Long id;

    /**
     * Электронная почта пользователя.
     */
    private String email;

    /**
     * Список ролей пользователя.
     */
    private List<String> roles;

    /**
     * Токен доступа.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Токен обновления.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * Тип токена.
     */
    @JsonProperty("token_type")
    private String tokenType;

}
