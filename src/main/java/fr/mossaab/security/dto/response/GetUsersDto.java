package fr.mossaab.security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO для получения информации о пользователях.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersDto {

    /**
     * Имя пользователя.
     */
    private String firstname;

    /**
     * Электронная почта пользователя.
     */
    private String email;

    /**
     * Фамилия пользователя.
     */
    private String lastname;

    /**
     * Номер телефона пользователя.
     */
    private String phoneNumber;

    /**
     * Роль работника.
     */
    private String workerRole;

    /**
     * Дата рождения пользователя.
     */
    private String dateOfBirth;

    /**
     * Фотография пользователя.
     */
    private String photo;

    /**
     * Код активации.
     */
    private Boolean activationCode;

    /**
     * Роль пользователя.
     */
    private String role;

    /**
     * Уникальный идентификатор пользователя.
     */
    private String id;
    private int balance;
}
