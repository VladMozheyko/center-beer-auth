package fr.mossaab.security.backup.project.mappers.v1;


import fr.mossaab.security.backup.core.mappers.BackupMapper;
import fr.mossaab.security.backup.project.dto.v1.LocationBackupDtoV1;
import fr.mossaab.security.entities.Location;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationBackupMapperV1 extends BackupMapper<Location, LocationBackupDtoV1> {
    
    LocationBackupDtoV1 toDto(Location location);
}
