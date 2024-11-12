package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Characteristic;
import fr.mossaab.security.entities.Unit;
import fr.mossaab.security.repository.CharacteristicRepository;
import fr.mossaab.security.repository.UnitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/unit")
@Tag(name = "Единицы измерения", description = "API для работы с единицами измерения характеристик")
@RequiredArgsConstructor
public class UnitController {

    private final UnitRepository unitRepository;
    private final CharacteristicRepository characteristicRepository;

    // DTO для вывода данных
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UnitDto {
        private Long id;
        private String shortName;
        private String longName;
        private List<Long> characteristicIds;

        // Конструкторы, геттеры и сеттеры
    }

    // 1) Метод, который выводит все unit и к каждому юниту идентификаторы характеристик
    @GetMapping("/get-all")
    @Operation(summary = "Получить все единицы измерения и идентификаторы характеристик принадлежащих к каждой серии")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список единиц измерения")
    })
    public List<UnitDto> getAllUnits() {
        List<Unit> units = unitRepository.findAll();
        List<UnitDto> result = new ArrayList<>();

        for (Unit unit : units) {
            UnitDto dto = new UnitDto();
            dto.setId(unit.getId());
            dto.setShortName(unit.getShortName());
            dto.setLongName(unit.getLongName());
            List<Long> characteristicIds = new ArrayList<>();

            for (Characteristic characteristic : unit.getCharacteristics()) {
                characteristicIds.add(characteristic.getId());
            }

            dto.setCharacteristicIds(characteristicIds);
            result.add(dto);
        }

        return result;
    }

    // 2) Создание нового unit без связной сущности
    @PostMapping("/add-unit")
    @Operation(summary = "Создать новую единицу измерения без прикрепления характеристики")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Единица измерения создана")
    })
    public Unit createUnit(@RequestBody UnitDto unitDto) {
        Unit unit = Unit.builder()
                .shortName(unitDto.getShortName())
                .longName(unitDto.getLongName())
                .characteristics(new ArrayList<>())
                .build();
        return unitRepository.save(unit);
    }

    // 3) Обновление полей unit и добавление характеристики
    @PutMapping("/update/{id}")
    @Operation(summary = "Обновить данные единицы измерения прикрепляя характеристику")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Единица измерения обновлена"),
            @ApiResponse(responseCode = "404", description = "Единица измерения не найдена")
    })
    public Unit updateUnit(@PathVariable Long id, @RequestBody UnitDto unitDto) {
        Optional<Unit> optionalUnit = unitRepository.findById(id);
        if (optionalUnit.isEmpty()) {
            throw new RuntimeException("Unit not found");
        }

        Unit unit = optionalUnit.get();
        if (unitDto.getShortName() != null) {
            unit.setShortName(unitDto.getShortName());
        }
        if (unitDto.getLongName() != null) {
            unit.setLongName(unitDto.getLongName());
        }

        // Добавление характеристики
        if (unitDto.getCharacteristicIds() != null && !unitDto.getCharacteristicIds().isEmpty()) {
            for (Long characteristicId : unitDto.getCharacteristicIds()) {
                Optional<Characteristic> characteristicOptional = characteristicRepository.findById(characteristicId);
                characteristicOptional.ifPresent(unit.getCharacteristics()::add);
            }
        }

        return unitRepository.save(unit);
    }

    // 4) Удаление unit по идентификатору
    @DeleteMapping("/delete-by-id/{id}")
    @Operation(summary = "Удалить единицу измерения по идентификаторы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Единица измерения удалена"),
            @ApiResponse(responseCode = "404", description = "Единица измерения не найдена")
    })
    public void deleteUnit(@PathVariable Long id) {
        Optional<Unit> unit = unitRepository.findById(id);
        if (unit.isEmpty()) {
            throw new RuntimeException("Unit not found");
        }
        unitRepository.delete(unit.get());
    }

    // 5) Поиск unit по идентификатору и вывод всех полей и идентификаторов характеристик
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Получить единицу измерения по идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Единица измерения найдена"),
            @ApiResponse(responseCode = "404", description = "Единица измерения не найдена")
    })
    public UnitDto getUnitById(@PathVariable Long id) {
        Optional<Unit> optionalUnit = unitRepository.findById(id);
        if (optionalUnit.isEmpty()) {
            throw new RuntimeException("Unit not found");
        }

        Unit unit = optionalUnit.get();
        UnitDto dto = new UnitDto();
        dto.setId(unit.getId());
        dto.setShortName(unit.getShortName());
        dto.setLongName(unit.getLongName());

        List<Long> characteristicIds = new ArrayList<>();
        for (Characteristic characteristic : unit.getCharacteristics()) {
            characteristicIds.add(characteristic.getId());
        }
        dto.setCharacteristicIds(characteristicIds);

        return dto;
    }
}