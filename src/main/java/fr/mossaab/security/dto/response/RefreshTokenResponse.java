package fr.mossaab.security.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на запрос обновления токена.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

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
