package fr.mossaab.security.backup.project.dto.v1;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserBackupDtoV1 {

    private Long id;          // ID из исходной БД
    private Integer temporarySecondsBalance;
    private String nickname;
    private String email;
    private String password;
    private String phone;
    private String role;
    private boolean phoneVerified;
    private LocalDateTime createdAt;

    private Long locationOriginalId;   // связь на Location
}
