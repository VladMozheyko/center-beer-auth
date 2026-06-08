package fr.mossaab.security.backup.project.mappers.v1;


import fr.mossaab.security.backup.core.mappers.BackupMapper;
import fr.mossaab.security.backup.project.dto.v1.UserSocialAccountBackupDtoV1;
import fr.mossaab.security.entities.UserSocialAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserSocialAccountBackupMapperV1 extends BackupMapper<UserSocialAccount, UserSocialAccountBackupDtoV1> {

    @Mapping(source = "user.id", target = "userOriginalId")
    UserSocialAccountBackupDtoV1 toDto(UserSocialAccount userSocialAccount);
}
