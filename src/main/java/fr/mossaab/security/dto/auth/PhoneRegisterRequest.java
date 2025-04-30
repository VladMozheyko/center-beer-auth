package fr.mossaab.security.dto.auth;

import fr.mossaab.security.enums.RegistrationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhoneRegisterRequest {

    @Schema(description = "Никнейм пользователя", example = "АмурскийТигр1995")
    private String nickname;

    @Schema(description = "Электронная почта пользователя", example = "example@gmail.com")
    private String email;

    @Schema(description = "Пароль пользователя", example = "Sasha123!")
    private String password;

    @Schema(description = "Номер телефона пользователя", example = "+79955941557")
    private String phone;

    @Schema(description = "Метод подтверждения регистрации (CALL — звонок, SMS — SMS-сообщение)", example = "CALL")
    private RegistrationMethod method;
}