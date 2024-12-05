package fr.mossaab.security.dto.response;

import lombok.Data;

@Data
public class AnswerGetProfile {
    private String phone;
    private String dateOfBirth;
    private String typeOfWorker;
    private String firstName;
    private String lastName;
    private String email;
    private String photo;
    private Long userId;
    private int balance; // Баланс пользователя
    private boolean isVerified; // Статус верификации документов
}