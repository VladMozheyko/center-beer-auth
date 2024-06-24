package fr.mossaab.security.mapper;


import fr.mossaab.security.entities.Type;
import fr.mossaab.security.dto.request.TypeDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TypeMapper {

    TypeDto map(Type type);

    @InheritInverseConfiguration
    Type map(TypeDto dto);
}
