package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivateResponse {
    private String status;
    private String notify;
    private String answer;
    private ErrorActivateDto errors;

    // Геттеры и сеттеры
}
