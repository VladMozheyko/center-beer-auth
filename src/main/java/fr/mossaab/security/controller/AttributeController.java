package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Attribute;
import fr.mossaab.security.entities.Series;

import fr.mossaab.security.repository.AttributeRepository;
import fr.mossaab.security.repository.SeriesRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Аттрибуты", description = "API для работы с аттрибутами")
@RestController
@SecurityRequirements
@RequiredArgsConstructor
@RequestMapping("/attribute")
public class AttributeController {

    private final AttributeRepository attributeRepository;
    private final SeriesRepository seriesRepository;

    @Operation(summary = "Поиск атрибута по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<AttributeDto> getAttributeById(@PathVariable Long id) {
        Optional<Attribute> attributeOptional = attributeRepository.findById(id);
        if (attributeOptional.isPresent()) {
            Attribute attribute = attributeOptional.get();
            AttributeDto attributeDto = new AttributeDto(attribute.getId(), attribute.getTitle(),
                    attribute.getSeries() != null ? attribute.getSeries().getId() : null);
            return ResponseEntity.ok(attributeDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Вывод всех атрибутов")
    @GetMapping("/get-all")
    public ResponseEntity<List<AttributeDto>> getAllAttributes() {
        List<Attribute> attributes = attributeRepository.findAll();
        List<AttributeDto> attributeDtos = new ArrayList<>();

        for (Attribute attribute : attributes) {
            attributeDtos.add(new AttributeDto(attribute.getId(), attribute.getTitle(),
                    attribute.getSeries() != null ? attribute.getSeries().getId() : null));
        }

        return ResponseEntity.ok(attributeDtos);
    }

    @Operation(summary = "Создание нового атрибута")
    @PostMapping("/add")
    public ResponseEntity<AttributeDto> createAttribute(@RequestBody CreateAttributeDto createAttributeDto) {
        Attribute attribute = new Attribute();
        attribute.setTitle(createAttributeDto.getTitle());
        attribute = attributeRepository.save(attribute);

        AttributeDto attributeDto = new AttributeDto(attribute.getId(), attribute.getTitle(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(attributeDto);
    }

    @Operation(summary = "Обновление атрибута")
    @PutMapping("/update/{id}")
    public ResponseEntity<AttributeDto> updateAttribute(@PathVariable Long id, @RequestBody UpdateAttributeDto updateAttributeDto) {
        Optional<Attribute> attributeOptional = attributeRepository.findById(id);
        if (attributeOptional.isPresent()) {
            Attribute attribute = attributeOptional.get();

            // Обновление полей атрибута
            if (updateAttributeDto.getTitle() != null) {
                attribute.setTitle(updateAttributeDto.getTitle());
            }

            // Обновление связной сущности
            if (updateAttributeDto.getSeriesId() != null) {
                Optional<Series> seriesOptional = seriesRepository.findById(updateAttributeDto.getSeriesId());
                if (seriesOptional.isPresent()) {
                    attribute.setSeries(seriesOptional.get());
                }
            }

            attribute = attributeRepository.save(attribute);
            AttributeDto attributeDto = new AttributeDto(attribute.getId(), attribute.getTitle(),
                    attribute.getSeries() != null ? attribute.getSeries().getId() : null);
            return ResponseEntity.ok(attributeDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Удаление атрибута по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteAttribute(@PathVariable Long id) {
        if (attributeRepository.existsById(id)) {
            attributeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // DTO classes
    @Data
    @AllArgsConstructor
    static class AttributeDto {
        private Long id;
        @Schema(example = "Высококачественная латунная гидрогруппа")
        private String title;
        private Long seriesId; // ID связной сущности, если она есть
    }

    @Data
    static class CreateAttributeDto {
        @Schema(example = "Высококачественная латунная гидрогруппа")
        private String title;
    }

    @Data
    static class UpdateAttributeDto {
        @Schema(example = "Высококачественная латунная гидрогруппа",nullable = true)
        private String title;
        @Schema(nullable = true)
        private Long seriesId; // ID связной сущности, если нужно обновить
    }
}
