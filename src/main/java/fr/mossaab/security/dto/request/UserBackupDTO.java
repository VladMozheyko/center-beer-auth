package fr.mossaab.security.dto.request;

import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import lombok.Data;

import java.util.Date;
import java.util.List;


@Data
public class UserBackupDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String phoneNumber;
    private WorkerRole workerRoles;
    private Date dateOfBirth;
    private Role role;
    private String activationCode;
    private FileDataDTO fileData; // DTO для FileData
    private List<RefreshTokenDTO> refreshTokens;
}