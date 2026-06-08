package fr.mossaab.security.integration.controller;


import fr.mossaab.security.dto.CityDto;
import fr.mossaab.security.dto.CountryDto;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuideControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Получение всех стран без фильтрации")
    void getAllCountries_ShouldReturnAllCountries() throws Exception {
        CountryDto[] countries = { new CountryDto(1L, "Россия"), new CountryDto(2L,"США") };
        Mockito.when(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok(countries));

        mockMvc.perform(get("/guide/countries/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].name").value("Россия"));
    }

    @Test
    @DisplayName("Получение всех городов с фильтрацией по стране")
    void getAllCitiesByCountry_ShouldReturnFilteredCities() throws Exception {
        CityDto[] cities = { new CityDto(1L, "Москва", 1L), new CityDto(2L, "Санкт-Петербург", 1L) };
        Mockito.when(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok(cities));

        mockMvc.perform(get("/guide/cities/all?countryId=179"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].name").value("Москва"));
    }
}
