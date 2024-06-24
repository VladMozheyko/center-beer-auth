package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String status;
    private String notify;
    private String answer;
    private ErrorsRegisterDto errors; // Здесь будет объект для хранения ошибок

    // Геттеры и сеттеры
}
