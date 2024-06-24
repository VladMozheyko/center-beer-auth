package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditProfileResponse {
    private String status;
    private String notify;
    private String answer;
    private ErrorsEditProfileDto errors; // Объект для хранения ошибок

    // Геттеры и сеттеры
}