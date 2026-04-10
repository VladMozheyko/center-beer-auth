package fr.mossaab.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mossaab.security.validation.annotation.ValidId;
import fr.mossaab.security.validation.annotation.ValidLocationName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO для представления города в системе")
public class CityDto {
    @Schema(description = "Уникальный идентификатор города в системе (может быть пустым)")
    @ValidId
    private Long id;

    @Schema(description = "Название города (может быть пустым)")
    @ValidLocationName
    private String name;

    @Schema(description = "Идентификатор страны, к которой относится город (может быть пустым)")
    @JsonProperty("country_id")
    @ValidId
    private Long countryId;
}
