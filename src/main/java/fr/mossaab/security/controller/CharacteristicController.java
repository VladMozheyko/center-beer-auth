package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Characteristic;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Unit;
import fr.mossaab.security.repository.CharacteristicRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.UnitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/characteristic")
@Tag(name = "Характеристики", description = "API для работы с характеристиками серии")
@RequiredArgsConstructor
public class CharacteristicController {

    private final CharacteristicRepository characteristicRepository;
    private final UnitRepository unitRepository;
    private final SeriesRepository seriesRepository;
    @Data
    public static class CharacteristicCreateDto {
        @Schema(example = "Макс./мин. тепловая мощность в режиме отопление")
        private String title;
    }
    // DTO для вывода данных
    @Data
    public static class CharacteristicDto {
        private Long id;
        @Schema(example = "Макс./мин. тепловая мощность в режиме отопление")
        private String title;
        private List<Long> unitIds;
        private List<Long> seriesIds;

        // Конструкторы, геттеры и сеттеры
    }

    @Data
    public static class CharacteristicUpdateDto {
        @Schema(example = "Макс./мин. тепловая мощность в режиме отопление", nullable = true)
        private String title;
        @Schema(nullable = true)
        private List<Long> unitIds;
        @Schema(nullable = true)
        private List<Long> seriesIds;
    }

    // 1) Метод, который выводит все Characteristic с идентификаторами Units и Series
    @GetMapping("/get-all")
    @Operation(summary = "Получить все характеристики")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список характеристик")
    })
    public List<CharacteristicDto> getAllCharacteristics() {
        List<Characteristic> characteristics = characteristicRepository.findAll();
        List<CharacteristicDto> result = new ArrayList<>();

        for (Characteristic characteristic : characteristics) {
            CharacteristicDto dto = new CharacteristicDto();
            dto.setId(characteristic.getId());
            dto.setTitle(characteristic.getTitle());

            // Добавляем идентификаторы связанных Unit
            List<Long> unitIds = new ArrayList<>();
            for (Unit unit : characteristic.getUnits()) {
                unitIds.add(unit.getId());
            }
            dto.setUnitIds(unitIds);

            // Добавляем идентификаторы связанных Series
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : characteristic.getSeries()) {
                seriesIds.add(series.getId());
            }
            dto.setSeriesIds(seriesIds);

            result.add(dto);
        }

        return result;
    }

    // 2) Создание нового Characteristic без связных сущностей
    @PostMapping("/add-characteristic")
    @Operation(summary = "Создать новую характеристику")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Характеристика создана")
    })
    public Characteristic createCharacteristic(@RequestBody CharacteristicCreateDto characteristicCreateDto) {
        Characteristic characteristic = Characteristic.builder()
                .title(characteristicCreateDto.getTitle())
                .units(new ArrayList<>())
                .series(new ArrayList<>())
                .build();
        return characteristicRepository.save(characteristic);
    }

    // 3) Обновление полей Characteristic и добавление Unit или Series
    @PutMapping("/update/{id}")
    @Operation(summary = "Обновить данные характеристики")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Характеристика обновлена"),
            @ApiResponse(responseCode = "404", description = "Характеристика не найдена")
    })
    public Characteristic updateCharacteristic(@PathVariable Long id, @RequestBody CharacteristicUpdateDto characteristicDto) {
        Optional<Characteristic> optionalCharacteristic = characteristicRepository.findById(id);
        if (optionalCharacteristic.isEmpty()) {
            throw new RuntimeException("Characteristic not found");
        }

        Characteristic characteristic = optionalCharacteristic.get();
        if (characteristicDto.getTitle() != null) {
            characteristic.setTitle(characteristicDto.getTitle());
        }

        // Добавление юнитов
        if (characteristicDto.getUnitIds() != null && !characteristicDto.getUnitIds().isEmpty()) {
            for (Long unitId : characteristicDto.getUnitIds()) {
                Optional<Unit> unitOptional = unitRepository.findById(unitId);
                unitOptional.ifPresent(characteristic.getUnits()::add);
            }
        }

        // Добавление серий
        if (characteristicDto.getSeriesIds() != null && !characteristicDto.getSeriesIds().isEmpty()) {
            for (Long seriesId : characteristicDto.getSeriesIds()) {
                Optional<Series> seriesOptional = seriesRepository.findById(seriesId);
                seriesOptional.ifPresent(characteristic.getSeries()::add);
            }
        }

        return characteristicRepository.save(characteristic);
    }

    // 4) Удаление Characteristic по идентификатору
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Удалить характеристику")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Характеристика удалена"),
            @ApiResponse(responseCode = "404", description = "Характеристика не найдена")
    })
    public void deleteCharacteristic(@PathVariable Long id) {
        Optional<Characteristic> characteristic = characteristicRepository.findById(id);
        if (characteristic.isEmpty()) {
            throw new RuntimeException("Characteristic not found");
        }
        characteristicRepository.delete(characteristic.get());
    }

    // 5) Поиск Characteristic по идентификатору и вывод всех полей и идентификаторов связных сущностей
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Получить характеристику по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Характеристика найдена"),
            @ApiResponse(responseCode = "404", description = "Характеристика не найдена")
    })
    public CharacteristicDto getCharacteristicById(@PathVariable Long id) {
        Optional<Characteristic> optionalCharacteristic = characteristicRepository.findById(id);
        if (optionalCharacteristic.isEmpty()) {
            throw new RuntimeException("Characteristic not found");
        }

        Characteristic characteristic = optionalCharacteristic.get();
        CharacteristicDto dto = new CharacteristicDto();
        dto.setId(characteristic.getId());
        dto.setTitle(characteristic.getTitle());

        // Идентификаторы связанных Unit
        List<Long> unitIds = new ArrayList<>();
        for (Unit unit : characteristic.getUnits()) {
            unitIds.add(unit.getId());
        }
        dto.setUnitIds(unitIds);

        // Идентификаторы связанных Series
        List<Long> seriesIds = new ArrayList<>();
        for (Series series : characteristic.getSeries()) {
            seriesIds.add(series.getId());
        }
        dto.setSeriesIds(seriesIds);

        return dto;
    }
}
