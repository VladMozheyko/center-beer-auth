package fr.mossaab.security.controller;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Boiler;
import fr.mossaab.security.entities.Value;
import fr.mossaab.security.repository.BoilerRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.ValueRepository;
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

    private final BoilerRepository boilerRepository;
    private final SeriesRepository seriesRepository;
    private final ValueRepository valueRepository;
    @Getter
    @Setter
    public static class BoilerCreateDTO {
        @Schema(example = "100")
        private Long number;
        @Schema(example = "4640270282360")
        private Long barcode;
    }
    // DTO for Boiler
    @Getter
    @Setter
    public static class BoilerDTO {
        private Long id;
        @Schema(example = "100")
        private Long number;
        @Schema(example = "4640270282360")
        private Long barcode;
        private Long seriesId;
        private List<Long> valueIds;

        // Constructor to map Boiler entity to DTO
        public BoilerDTO(Boiler boiler) {
            this.id = boiler.getId();
            this.number = boiler.getNumber();
            this.barcode = boiler.getBarcode();
            this.seriesId = boiler.getSeries() != null ? boiler.getSeries().getId() : null;
            this.valueIds = new ArrayList<>();
            for (Value value : boiler.getValues()) {
                this.valueIds.add(value.getId());
            }
        }
    }

    // 1. Get all boilers with related entities
    @Operation(summary = "Вывод всех бойлеров", description = "Вывести все бойлеры, все поля и идентификаторы всех связных сущностей")
    @GetMapping("/find-all")
    public List<BoilerDTO> getAllBoilers() {
        List<BoilerDTO> boilerDTOs = new ArrayList<>();
        List<Boiler> boilers = boilerRepository.findAll();
        for (Boiler boiler : boilers) {
            boilerDTOs.add(new BoilerDTO(boiler));
        }
        return boilerDTOs;
    }

    // 2. Get a boiler by ID with its related entities
    @Operation(summary = "Поиск по идентификатору", description = "Найти бойлер по ID и показать идентификаторы всех связных сущностей")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<BoilerDTO> getBoilerById(@PathVariable Long id) {
        Optional<Boiler> boilerOptional = boilerRepository.findById(id);
        if (boilerOptional.isPresent()) {
            return ResponseEntity.ok(new BoilerDTO(boilerOptional.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 3. Get a boiler by barcode
    @Operation(summary = "Поиск по штрих коду", description = "Найти бойлер по штрих коду")
    @GetMapping("/find-by-barcode/barcode/{barcode}")
    public ResponseEntity<BoilerDTO> getBoilerByBarcode(@PathVariable Long barcode) {
        Optional<Boiler> boilerOptional = boilerRepository.findByBarcode(barcode);
        if (boilerOptional.isPresent()) {
            return ResponseEntity.ok(new BoilerDTO(boilerOptional.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 4. Create a new boiler without related entities
    @Operation(summary = "Создание нового бойлера", description = "Создать новый бойлер с полями без связных сущностей")
    @PostMapping("/add")
    public ResponseEntity<BoilerDTO> createBoiler(@RequestBody BoilerCreateDTO boilerCreateDTO) {
        Boiler boiler = new Boiler();
        boiler.setNumber(boilerCreateDTO.getNumber());
        boiler.setBarcode(boilerCreateDTO.getBarcode());
        boiler = boilerRepository.save(boiler);
        return new ResponseEntity<>(new BoilerDTO(boiler), HttpStatus.CREATED);
    }

    // 5. Update a boiler and link it to existing related entities
    @Operation(summary = "Обновление бойлера", description = "Обновить бойлер и добавить существующие связные сущности")
    @PatchMapping("/update/{id}")
    public ResponseEntity<BoilerDTO> updateBoiler(
            @PathVariable Long id,
            @RequestBody BoilerDTO boilerDTO,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) List<Long> valueIds) {

        Optional<Boiler> boilerOptional = boilerRepository.findById(id);
        if (boilerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Boiler boiler = boilerOptional.get();

        // Update fields if present
        if (boilerDTO.getNumber() != null) boiler.setNumber(boilerDTO.getNumber());
        if (boilerDTO.getBarcode() != null) boiler.setBarcode(boilerDTO.getBarcode());

        // Link to an existing Series if ID is provided
        if (seriesId != null) {
            Optional<Series> seriesOptional = seriesRepository.findById(seriesId);
            seriesOptional.ifPresent(boiler::setSeries);
        }

        // Link to existing Values if IDs are provided
        if (valueIds != null) {
            List<Value> values = new ArrayList<>();
            for (Long valueId : valueIds) {
                Optional<Value> valueOptional = valueRepository.findById(valueId);
                valueOptional.ifPresent(values::add);
            }
            boiler.setValues(values);
        }

        boiler = boilerRepository.save(boiler);
        return ResponseEntity.ok(new BoilerDTO(boiler));
    }

    // 6. Delete a boiler by ID
    @Operation(summary = "Удаление бойлера по идентификатору", description = "Удалить бойлер по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteBoilerById(@PathVariable Long id) {
        if (boilerRepository.existsById(id)) {
            boilerRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 7. Delete a boiler by barcode
    @Operation(summary = "Удаление бойлера по штрих коду", description = "Удалить бойлер по штрих коду")
    @DeleteMapping("/delete-by-barcode/{barcode}")
    public ResponseEntity<Void> deleteBoilerByBarcode(@PathVariable Long barcode) {
        Optional<Boiler> boilerOptional = boilerRepository.findByBarcode(barcode);
        if (boilerOptional.isPresent()) {
            boilerRepository.delete(boilerOptional.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
