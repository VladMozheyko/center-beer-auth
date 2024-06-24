package fr.mossaab.security.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запрос на редактирование профиля пользователя.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditProfileDto {

    /**
     * Имя пользователя.
     */
    private String username;

    /**
     * Фамилия пользователя.
     */
    private String lastname;

    /**
     * Дата рождения пользователя в формате строки.
     */
    private String dateOfBirth;
}
