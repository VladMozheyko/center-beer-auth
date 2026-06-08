package fr.mossaab.security.backup.project.mappers.v1;


import fr.mossaab.security.backup.core.mappers.BackupMapper;
import fr.mossaab.security.backup.project.dto.v1.FileDataBackupDtoV1;
import fr.mossaab.security.entities.FileData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileDataBackupMapperV1 extends BackupMapper<FileData, FileDataBackupDtoV1> {

    @Mapping(source = "user.id", target = "userOriginalId")
    FileDataBackupDtoV1 toDto(FileData fileData);
}
