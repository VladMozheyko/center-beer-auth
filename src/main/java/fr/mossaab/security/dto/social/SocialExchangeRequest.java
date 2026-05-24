package fr.mossaab.security.dto.social;

import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.validation.annotation.ValidAuthCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SocialExchangeRequest {

    @Schema(description = "Одноразовый code, полученный после social-login/init", example = "ABCDEF12-3456-7890-abcd-ef1234567890")
    @ValidAuthCode
    private String authCode;

    @Schema(description = "Провайдер: GOOGLE/YANDEX/VK")
    @NotNull(message = "Провайдер авторизации обязателен для заполнения")
    private OAuthProvider provider;

    @Schema(description = "ID устройства полученный в ответе ранее с jwt (может быть пустым)")
    private String deviceId;
}
