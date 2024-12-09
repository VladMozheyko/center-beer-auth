package fr.mossaab.security.service;

import fr.mossaab.security.entities.Attribute;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.AttributeRepository;
import fr.mossaab.security.repository.SeriesRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeRepository attributeRepository;
    private final SeriesRepository seriesRepository;

    public ResponseEntity<AttributeDto> getAttributeById(Long id) {
        Optional<Attribute> attributeOptional = attributeRepository.findById(id);
        if (attributeOptional.isPresent()) {
            Attribute attribute = attributeOptional.get();
            return ResponseEntity.ok(mapToDto(attribute));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    public List<AttributeDto> getAllAttributes() {
        List<Attribute> attributes = attributeRepository.findAll();
        List<AttributeDto> attributeDtos = new ArrayList<>();
        for (Attribute attribute : attributes) {
            attributeDtos.add(mapToDto(attribute));
        }
        return attributeDtos;
    }

    public ResponseEntity<AttributeDto> createAttribute(CreateAttributeDto createAttributeDto) {
        Attribute attribute = new Attribute();
        attribute.setTitle(createAttributeDto.getTitle());
        attribute = attributeRepository.save(attribute);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(attribute));
    }

    public ResponseEntity<AttributeDto> updateAttribute(Long id, UpdateAttributeDto updateAttributeDto) {
        Optional<Attribute> attributeOptional = attributeRepository.findById(id);
        if (attributeOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Attribute attribute = attributeOptional.get();
        if (updateAttributeDto.getTitle() != null) {
            attribute.setTitle(updateAttributeDto.getTitle());
        }

        if (updateAttributeDto.getSeriesId() != null) {
            Optional<Series> seriesOptional = seriesRepository.findById(updateAttributeDto.getSeriesId());
            seriesOptional.ifPresent(attribute::setSeries);
        }

        attribute = attributeRepository.save(attribute);
        return ResponseEntity.ok(mapToDto(attribute));
    }

    public ResponseEntity<Void> deleteAttribute(Long id) {
        Optional<Attribute> attributeOptional = attributeRepository.findById(id);
        if (attributeOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Attribute attribute = attributeOptional.get();
        if (attribute.getSeries() != null) {
            attribute.setSeries(null);
            attributeRepository.save(attribute);
        }

        attributeRepository.delete(attribute);
        return ResponseEntity.noContent().build();
    }

    private AttributeDto mapToDto(Attribute attribute) {
        return new AttributeDto(
                attribute.getId(),
                attribute.getTitle(),
                attribute.getSeries() != null ? attribute.getSeries().getId() : null
        );
    }

    // DTO classes
    @Data
    @AllArgsConstructor
    public static class AttributeDto {
        private Long id;
        @Schema(example = "Высококачественная латунная гидрогруппа")
        private String title;
        private Long seriesId; // ID связной сущности, если она есть
    }

    @Data
    public static class CreateAttributeDto {
        @Schema(example = "Высококачественная латунная гидрогруппа")
        private String title;
    }

    @Data
    public static class UpdateAttributeDto {
        @Schema(example = "Высококачественная латунная гидрогруппа",nullable = true)
        private String title;
        @Schema(nullable = true)
        private Long seriesId; // ID связной сущности, если нужно обновить
    }
}
