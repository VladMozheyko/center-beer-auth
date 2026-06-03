package fr.mossaab.security.backup.project.dto.v1;


import lombok.Data;

@Data
public class UserSocialAccountBackupDtoV1 {

    private Long id;           // ID из исходной БД
    private String provider;

    private String externalId;
    private String socialEmail;

    private Long userOriginalId;
}
