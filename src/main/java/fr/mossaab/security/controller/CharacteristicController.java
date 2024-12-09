package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Characteristic;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Unit;
import fr.mossaab.security.repository.CharacteristicRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.UnitRepository;
import fr.mossaab.security.service.CharacteristicService;
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
    private final CharacteristicService characteristicService;

    @GetMapping("/get-all")
    @Operation(summary = "Получить все характеристики")
    public List<CharacteristicService.CharacteristicDto> getAllCharacteristics() {
        return characteristicService.getAllCharacteristics();
    }

    @PostMapping("/add-characteristic")
    @Operation(summary = "Создать новую характеристику")
    public Characteristic createCharacteristic(@RequestBody CharacteristicService.CharacteristicCreateDto characteristicCreateDto) {
        return characteristicService.createCharacteristic(characteristicCreateDto);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Обновить данные характеристики")
    public Characteristic updateCharacteristic(@PathVariable Long id, @RequestBody CharacteristicService.CharacteristicUpdateDto characteristicDto) {
        return characteristicService.updateCharacteristic(id, characteristicDto);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Удалить характеристику")
    public void deleteCharacteristic(@PathVariable Long id) {
        characteristicService.deleteCharacteristic(id);
    }

    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Получить характеристику по ID")
    public CharacteristicService.CharacteristicDto getCharacteristicById(@PathVariable Long id) {
        return characteristicService.getCharacteristicById(id);
    }

}