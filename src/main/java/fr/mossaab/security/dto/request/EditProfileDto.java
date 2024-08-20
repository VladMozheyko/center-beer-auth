package fr.mossaab.security.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Запрос на редактирование профиля пользователя.
 */
@Data
public class EditProfileDto {
    private MultipartFile image;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String dateOfBirth;
}