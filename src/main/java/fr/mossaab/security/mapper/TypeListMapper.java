package fr.mossaab.security.mapper;

import fr.mossaab.security.entities.Type;
import fr.mossaab.security.payload.request.TypeDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = TypeMapper.class)
public interface TypeListMapper {
    // признак пустой бд

    List<TypeDto> toDTOList(List<Type> types);

    @InheritInverseConfiguration
    List<Type> toEntityList(List<TypeDto> typeDtos);
}
