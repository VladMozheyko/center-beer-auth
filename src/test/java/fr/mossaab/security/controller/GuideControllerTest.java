package fr.mossaab.security.controller;

import fr.mossaab.security.dto.CityDto;
import fr.mossaab.security.dto.CountryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuideControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GuideController guideController;

    @Nested
    @DisplayName("Страны")
    class Countries {
        CountryDto countryDto1 = new CountryDto();
        CountryDto countryDto2 = new CountryDto();

        @BeforeEach
        public void setUp() throws Exception {
            countryDto1.setName("Россия");
            countryDto2.setName("Франция");
        }

        @Test
        @DisplayName("Возврат всех стран без фильтра")
        void countries_WhenNoName_ReturnsAllCountries() {
            CountryDto[] countries = {countryDto1, countryDto2};
            String expectedUrl = "https://center.beer/api/v2/getCountries";
            when(restTemplate.getForEntity(expectedUrl, CountryDto[].class))
                    .thenReturn(new ResponseEntity<>(countries, HttpStatus.OK));

            List<CountryDto> result = guideController.countries(null);

            verify(restTemplate, times(1)).getForEntity(expectedUrl, CountryDto[].class);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Россия");
        }

        @Test
        @DisplayName("Возврат стран c фильтром по названию")
        void countries_WithName_ReturnsFilteredCountries() {
            CountryDto[] countries = {countryDto1};
            String expectedUrl = "https://center.beer/api/v2/getCountries?name=Россия";
            when(restTemplate.getForEntity(expectedUrl, CountryDto[].class))
                    .thenReturn(new ResponseEntity<>(countries, HttpStatus.OK));

            List<CountryDto> result = guideController.countries("Россия");

            verify(restTemplate).getForEntity(expectedUrl, CountryDto[].class);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Россия");
        }

        @Test
        @DisplayName("Возврат пустого списка, если снаружи null результат")
        void countries_WhenResponseBodyNull_ReturnsEmptyList() {
            String expectedUrl = "https://center.beer/api/v2/getCountries";
            when(restTemplate.getForEntity(expectedUrl, CountryDto[].class))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            List<CountryDto> result = guideController.countries(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Города")
    class Cities {

        CityDto cityDto1 = new CityDto();
        CityDto cityDto2 = new CityDto();

        @BeforeEach
        public void setUp() throws Exception {
            cityDto1.setName("Москва");
            cityDto2.setCountryId(1L);
            cityDto2.setName("Париж");
            cityDto2.setCountryId(2L);
        }


        @Test
        @DisplayName("Все города без фильтров")
        void cities_WhenNoParams_ReturnsAllCities() {
            CityDto[] arr = {cityDto1, cityDto2};
            String expectedUrl = "https://center.beer/api/v2/getCities";
            when(restTemplate.getForEntity(expectedUrl, CityDto[].class))
                    .thenReturn(new ResponseEntity<>(arr, HttpStatus.OK));

            List<CityDto> result = guideController.cities(null, null);

            verify(restTemplate).getForEntity(expectedUrl, CityDto[].class);
            assertThat(result).hasSize(2);
            assertThat(result.get(1).getName()).isEqualTo("Париж");
        }

        @Test
        @DisplayName("Города c фильтром по названию")
        void cities_WithName_ReturnsFilteredCities() {
            CityDto[] cities = {cityDto1};
            String expectedUrl = "https://center.beer/api/v2/getCities?name=Москва";
            when(restTemplate.getForEntity(expectedUrl, CityDto[].class))
                    .thenReturn(new ResponseEntity<>(cities, HttpStatus.OK));

            List<CityDto> result = guideController.cities("Москва", null);

            verify(restTemplate).getForEntity(expectedUrl, CityDto[].class);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Москва");
        }

        @Test
        @DisplayName("Города c фильтром только по countryId")
        void cities_WithCountryId_ReturnsFilteredCities() {
            cityDto1.setCountryId(179L);
            cityDto1.setName("Минск");
            CityDto[] cities = {cityDto1};

            String expectedUrl = "https://center.beer/api/v2/getCities?country_id=179";
            when(restTemplate.getForEntity(expectedUrl, CityDto[].class))
                    .thenReturn(new ResponseEntity<>(cities, HttpStatus.OK));

            List<CityDto> result = guideController.cities(null, "179");

            verify(restTemplate).getForEntity(expectedUrl, CityDto[].class);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCountryId()).isEqualTo(179);
        }

        @Test
        @DisplayName("Города с двумя фильтрами")
        void cities_WithNameAndCountryId_ReturnsFilteredCities() {
            cityDto1.setCountryId(1L);
            cityDto1.setName("Минск");
            CityDto[] cities = {cityDto1};

            String expectedUrl = "https://center.beer/api/v2/getCities?name=Минск&country_id=179";
            when(restTemplate.getForEntity(expectedUrl, CityDto[].class))
                    .thenReturn(new ResponseEntity<>(cities, HttpStatus.OK));

            List<CityDto> result = guideController.cities("Минск", "179");

            verify(restTemplate).getForEntity(expectedUrl, CityDto[].class);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Пустой список городов при пустом ответе")
        void cities_WhenResponseBodyNull_ReturnsEmptyList() {
            String expectedUrl = "https://center.beer/api/v2/getCities";
            when(restTemplate.getForEntity(expectedUrl, CityDto[].class))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            List<CityDto> result = guideController.cities(null, null);

            assertThat(result).isEmpty();
        }
    }
}