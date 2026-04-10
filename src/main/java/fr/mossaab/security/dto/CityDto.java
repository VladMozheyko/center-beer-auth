package fr.mossaab.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CityDto {
    private Long id;
    private String name;
    @JsonProperty("country_id")
    private Long countryId;

}
