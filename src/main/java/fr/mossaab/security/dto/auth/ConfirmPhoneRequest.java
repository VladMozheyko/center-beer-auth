package fr.mossaab.security.dto.auth;

import fr.mossaab.security.validation.annotation.ValidRussianPhone;
import fr.mossaab.security.validation.annotation.ValidSmsCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPhoneRequest {

    @Schema(example = "+79955941557", description = "Телефон, который подтверждаем")
    @ValidRussianPhone
    private String phone;

    @Schema(example = "7072", description = "Код из звонка/СМС")
    @ValidSmsCode
    private String code;
}