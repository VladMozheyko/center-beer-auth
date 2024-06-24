package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String status;
    private String notify;
    private String answer;
    private ErrorsLoginDto errors; // Объект для хранения ошибок
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    // Геттеры и сеттеры
}