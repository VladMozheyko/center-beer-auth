package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Boiler;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Value;
import fr.mossaab.security.repository.BoilerRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.ValueRepository;
import fr.mossaab.security.service.BoilerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Бойлеры", description = "API для работы с бойлерами")
@RestController
@RequiredArgsConstructor
@RequestMapping("/boiler")
public class BoilerController {
    private final BoilerService boilerService;

    @Operation(summary = "Вывод всех бойлеров", description = "Вывести все бойлеры, все поля и идентификаторы всех связных сущностей")
    @GetMapping("/find-all")
    public List<BoilerService.BoilerDTO> getAllBoilers() {
        return boilerService.getAllBoilers();
    }

    @Operation(summary = "Поиск по идентификатору", description = "Найти бойлер по ID и показать идентификаторы всех связных сущностей")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<BoilerService.BoilerDTO> getBoilerById(@PathVariable Long id) {
        return boilerService.getBoilerById(id);
    }

    @Operation(summary = "Поиск по штрих коду", description = "Найти бойлер по штрих коду")
    @GetMapping("/find-by-barcode/barcode/{barcode}")
    public ResponseEntity<BoilerService.BoilerDTO> getBoilerByBarcode(@PathVariable Long barcode) {
        return boilerService.getBoilerByBarcode(barcode);
    }

    @Operation(summary = "Создание нового бойлера", description = "Создать новый бойлер с полями без связных сущностей")
    @PostMapping("/add")
    public ResponseEntity<BoilerService.BoilerDTO> createBoiler(@RequestBody BoilerService.BoilerCreateDTO boilerCreateDTO) {
        return boilerService.createBoiler(boilerCreateDTO);
    }

    @Operation(summary = "Обновление бойлера", description = "Обновить бойлер и добавить существующие связные сущности")
    @PutMapping("/update/{id}")
    public ResponseEntity<BoilerService.BoilerDTO> updateBoiler(
            @PathVariable Long id,
            @RequestBody BoilerService.BoilerUpdateDTO boilerDTO) {
        return boilerService.updateBoiler(id, boilerDTO);
    }

    @Operation(summary = "Удаление бойлера по идентификатору", description = "Удалить бойлер по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteBoilerById(@PathVariable Long id) {
        return boilerService.deleteBoilerById(id);
    }

}
