package fr.mossaab.security.backup.project.mappers.v1;


import fr.mossaab.security.backup.core.mappers.BackupMapper;
import fr.mossaab.security.backup.project.dto.v1.UserBackupDtoV1;
import fr.mossaab.security.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserBackupMapperV1 extends BackupMapper<User, UserBackupDtoV1> {

    @Mapping(source = "location.id", target = "locationOriginalId")
    UserBackupDtoV1 toDto(User user);
}
