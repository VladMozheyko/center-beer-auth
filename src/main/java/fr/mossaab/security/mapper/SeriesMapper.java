package fr.mossaab.security.mapper;


import fr.mossaab.security.entities.Series;
import fr.mossaab.security.payload.request.SeriesDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {KindMapper.class})
public interface SeriesMapper  {

    SeriesDto map(Series series);
    @InheritInverseConfiguration
    Series map(SeriesDto dto);
}
