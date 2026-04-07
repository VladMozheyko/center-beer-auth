package fr.mossaab.security.dto.social;

import fr.mossaab.security.enums.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SocialExchangeRequest {
    @Schema(description = "Одноразовый code, полученный после social-login/init")
    private String authCode;
    @Schema(description = "Провайдер: GOOGLE/YANDEX/VK")
    private OAuthProvider provider;
}
