package fr.mossaab.security.controller;

import fr.mossaab.security.dto.CityDto;
import fr.mossaab.security.dto.CountryDto;
import fr.mossaab.security.validation.annotation.ValidId;
import fr.mossaab.security.validation.annotation.ValidLocationName;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/guide")
@RequiredArgsConstructor
@Tag(name = "Справочники", description = "API для получения списка стран и городов")
public class GuideController {

    private final RestTemplate restTemplate;

    @Operation(summary = "Получить список стран", description = "Возвращает список всех стран, при передаче параметра name фильтрует по названию")
    @GetMapping("/countries/all")
    public List<CountryDto> countries(
            @Parameter(description = "Название страны для фильтрации (пример: Россия)")
            @RequestParam(required = false) @ValidLocationName
            String name
    ) {
        String baseUrl = "https://center.beer/api/v2/getCountries";

        if (name != null && !name.isBlank()) {
            baseUrl += "?name=" + name;
        }

        ResponseEntity<CountryDto[]> response =
                restTemplate.getForEntity(baseUrl, CountryDto[].class);

        CountryDto[] body = response.getBody();
        return body != null ? Arrays.asList(body) : List.of();
    }

    @Operation(summary = "Получить список городов", description = "Возвращает список всех городов, фильтрует по названию и/или ID страны")
    @GetMapping("/cities/all")
    public List<CityDto> cities(
            @Parameter(description = "Название города для фильтрации (пример: Москва)")
            @RequestParam(required = false) @ValidLocationName
            String name,

            @Parameter(description = "ID страны для фильтрации (пример: 179)")
            @RequestParam(required = false) @ValidId
            String countryId
    ) {
        String baseUrl = "https://center.beer/api/v2/getCities";

        if (name != null && !name.isBlank()) {
            baseUrl += "?name=" + name;
        }
        if (countryId != null && !countryId.isBlank()) {
            baseUrl += (baseUrl.contains("?") ? "&" : "?") + "country_id=" + countryId;
        }

        ResponseEntity<CityDto[]> response =
                restTemplate.getForEntity(baseUrl, CityDto[].class);

        CityDto[] body = response.getBody();
        return body != null ? Arrays.asList(body) : List.of();
    }
}
