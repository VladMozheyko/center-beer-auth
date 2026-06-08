package fr.mossaab.security.backup.project.dto.v1;

import lombok.Data;

import java.util.List;

@Data
public class BackupDataV1 {

    private List<UserBackupDtoV1> users;
    private List<FileDataBackupDtoV1> fileData;
    private List<LocationBackupDtoV1> locations;
    private List<UserSocialAccountBackupDtoV1> socialAccounts;
}
