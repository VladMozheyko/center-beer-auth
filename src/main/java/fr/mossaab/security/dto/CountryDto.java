package fr.mossaab.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO для страны")
public class CountryDto {

    @Schema(description = "Уникальный идентификатор страны")
    private Long id;

    @Schema(description = "Название страны")
    private String name;
}

