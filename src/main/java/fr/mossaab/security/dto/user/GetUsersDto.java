package fr.mossaab.security.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersDto {


    /**
     * Электронная почта пользователя.
     */
    private String email;

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

}