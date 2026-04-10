package fr.mossaab.security.dto.auth;

import fr.mossaab.security.validation.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailRequest {
    @ValidEmail
    @Schema(example = "example@ex.ru", description = "Эмейл пользователя")
    private String email;
}
