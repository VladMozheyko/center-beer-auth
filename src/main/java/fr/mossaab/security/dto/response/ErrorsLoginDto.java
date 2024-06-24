package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorsLoginDto {
    private String email;
    private String password;

    // Геттеры и сеттеры для каждого поля
}
