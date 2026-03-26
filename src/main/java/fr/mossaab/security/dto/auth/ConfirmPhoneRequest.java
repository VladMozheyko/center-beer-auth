package fr.mossaab.security.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfirmPhoneRequest {

    @Schema(example = "+79955941557", description = "Телефон, который подтверждаем")
    private String phone;

    @Schema(example = "7072", description = "Код из звонка/СМС")
    private String code;
}