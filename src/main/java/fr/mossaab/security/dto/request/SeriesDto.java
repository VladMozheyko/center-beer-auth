package fr.mossaab.security.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fr.mossaab.security.entities.Kind;
import lombok.Data;
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SeriesDto {

    private Long id;
    private String description;
    private Kind kind;
}
