package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {
    private String status;
    private String notify;
    private String answer;
    private ErrorPasswordResetDto errors;

    // Геттеры и сеттеры
}
