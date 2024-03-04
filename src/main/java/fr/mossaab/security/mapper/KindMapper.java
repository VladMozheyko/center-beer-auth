package fr.mossaab.security.mapper;


import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.payload.request.KindDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TypeMapper.class})
public interface KindMapper {

    KindDto map(Kind kind);
    @InheritInverseConfiguration
    Kind map(KindDto dto);
}
