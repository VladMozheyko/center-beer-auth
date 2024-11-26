package fr.mossaab.security.controller;

import fr.mossaab.security.entities.*;
import fr.mossaab.security.entities.Error;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.repository.BoilerSeriesPassportRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Серии", description = "API для работы с сериями")
@RestController
@SecurityRequirements()
@RequiredArgsConstructor
@RequestMapping("/series")
public class SeriesController {

    private final SeriesRepository seriesRepository;
    private final KindRepository kindRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final CharacteristicRepository characteristicRepository;
    private final AdvantageRepository advantageRepository;
    private final AttributeRepository attributeRepository;
    private final BoilerRepository boilerRepository;
    private final FileDataRepository fileDataRepository;
    private final ErrorRepository errorRepository;
    private final BoilerSeriesPassportRepository passportTitleRepository;
    private final StorageService storageService;
    @Data
    private static class SeriesCreateDto {
        @Schema(example = "T")
        private String prefix;
        @Schema(example = "100")
        private int startRange;
        @Schema(example = "240")
        private int endRange;
        @Schema(example = "K")
        private String suffix;
        @Schema(example = "Одноконтурные котлы, с закрытой камерой сгорания, с трёхходовым клапаном, модели (T10OK-T24OK)")
        private String description;
    }
    // DTOs
    @Data
    private static class SeriesDto {
        private Long id;
        @Schema(example = "T")
        private String prefix;
        @Schema(example = "100")
        private int startRange;
        @Schema(example = "240")
        private int endRange;
        @Schema(example = "K")
        private String suffix;
        @Schema(example = "Одноконтурные котлы, с закрытой камерой сгорания, с трёхходовым клапаном, модели (T10OK-T24OK)")
        private String description;
        private Long kindId;
        private List<Long> serviceCenterIds = new ArrayList<>();
        private List<Long> characteristicIds = new ArrayList<>();
        private List<Long> advantageIds = new ArrayList<>();
        private List<Long> attributeIds = new ArrayList<>();
        private List<Long> boilerIds = new ArrayList<>();
        private List<Long> fileDataIds = new ArrayList<>();
        private List<Long> errorIds = new ArrayList<>();
        private List<Long> passportTitleIds = new ArrayList<>();
    }


    // 1) Получение всех серий
    @Operation(summary = "Получить все серии")
    @GetMapping("/get-all-series")
    public List<SeriesDto> getAllSeries() {
        List<SeriesDto> result = new ArrayList<>();
        List<Series> seriesList = seriesRepository.findAll();

        for (Series series : seriesList) {
            SeriesDto dto = mapToDto(series);
            result.add(dto);
        }

        return result;
    }

    // 2) Поиск серии по идентификатору
    @Operation(summary = "Получить серию по идентификатору")
    @GetMapping("/get-series-by-id/{id}")
    public ResponseEntity<SeriesDto> getSeriesById(@PathVariable Long id) {
        Optional<Series> series = seriesRepository.findById(id);

        if (series.isPresent()) {
            return ResponseEntity.ok(mapToDto(series.get()));
        }

        return ResponseEntity.notFound().build();
    }

    // 3) Обновление серии
    @Operation(summary = "Обновить серию")
    @PutMapping("/update-series/{id}")
    public ResponseEntity<SeriesDto> updateSeries(@PathVariable Long id, @RequestBody SeriesDto seriesDto) {
        Optional<Series> optionalSeries = seriesRepository.findById(id);

        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();
            if (seriesDto.getPrefix() != null) series.setPrefix(seriesDto.getPrefix());
            if (seriesDto.getStartRange() != 0) series.setStartRange(seriesDto.getStartRange());
            if (seriesDto.getEndRange() != 0) series.setEndRange(seriesDto.getEndRange());
            if (seriesDto.getSuffix() != null) series.setSuffix(seriesDto.getSuffix());
            if (seriesDto.getDescription() != null) series.setDescription(seriesDto.getDescription());
            // Сохранение изменений
            seriesRepository.save(series);
            return ResponseEntity.ok(mapToDto(series));
        }

        return ResponseEntity.notFound().build();
    }

    // 4) Создание серии
    @Operation(summary = "Создать серию")
    @PostMapping(value = "/add-series", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public SeriesDto createSeries(@RequestBody SeriesCreateDto seriesCreateDto, @RequestPart MultipartFile image) throws IOException {
        Series series = new Series();
        series.setPrefix(seriesCreateDto.getPrefix());
        series.setStartRange(seriesCreateDto.getStartRange());
        series.setEndRange(seriesCreateDto.getEndRange());
        series.setSuffix(seriesCreateDto.getSuffix());
        series.setDescription(seriesCreateDto.getDescription());
        seriesRepository.save(series);

        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image, series);
        fileDataRepository.save(uploadImage);

        return mapToDto(series);
    }

    // 5) Обновление связных сущностей
    @Operation(summary = "Обновить связные сущности серии")
    @PutMapping("/update-related/{id}")
    public ResponseEntity<SeriesDto> updateRelatedEntities(@PathVariable Long id, @RequestBody SeriesDto seriesDto) {
        Optional<Series> optionalSeries = seriesRepository.findById(id);

        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();

            // Обновление Kind
            if (seriesDto.getKindId() != null) {
                kindRepository.findById(seriesDto.getKindId()).ifPresent(series::setKind);
            }

            // Обновление ServiceCenters
            if (seriesDto.getServiceCenterIds() != null) {
                series.getServiceCenters().clear();
                for (Long serviceCenterId : seriesDto.getServiceCenterIds()) {
                    serviceCenterRepository.findById(serviceCenterId).ifPresent(series.getServiceCenters()::add);
                }
            }

            // Обновление Characteristics
            if (seriesDto.getCharacteristicIds() != null) {
                series.getCharacteristics().clear();
                for (Long characteristicId : seriesDto.getCharacteristicIds()) {
                    characteristicRepository.findById(characteristicId).ifPresent(series.getCharacteristics()::add);
                }
            }

            // Обновление Advantages
            if (seriesDto.getAdvantageIds() != null) {
                series.getAdvantages().clear();
                for (Long advantageId : seriesDto.getAdvantageIds()) {
                    advantageRepository.findById(advantageId).ifPresent(series.getAdvantages()::add);
                }
            }

            // Обновление Attributes
            if (seriesDto.getAttributeIds() != null) {
                series.getAttributes().clear();
                for (Long attributeId : seriesDto.getAttributeIds()) {
                    attributeRepository.findById(attributeId).ifPresent(series.getAttributes()::add);
                }
            }

            // Обновление Boilers
            if (seriesDto.getBoilerIds() != null) {
                series.getBoilers().clear();
                for (Long boilerId : seriesDto.getBoilerIds()) {
                    boilerRepository.findById(boilerId).ifPresent(series.getBoilers()::add);
                }
            }

            // Обновление Files
            if (seriesDto.getFileDataIds() != null) {
                series.getFiles().clear();
                for (Long fileDataId : seriesDto.getFileDataIds()) {
                    fileDataRepository.findById(fileDataId).ifPresent(series.getFiles()::add);
                }
            }

            // Обновление Errors
            if (seriesDto.getErrorIds() != null) {
                series.getErrors().clear();
                for (Long errorId : seriesDto.getErrorIds()) {
                    errorRepository.findById(errorId).ifPresent(series.getErrors()::add);
                }
            }

            // Обновление PassportTitles
            if (seriesDto.getPassportTitleIds() != null) {
                series.getBoilerSeriesPassports().clear();
                for (Long passportTitleId : seriesDto.getPassportTitleIds()) {
                    passportTitleRepository.findById(passportTitleId).ifPresent(series.getBoilerSeriesPassports()::add);
                }
            }

            // Сохранение изменений
            seriesRepository.save(series);
            return ResponseEntity.ok(mapToDto(series));
        }

        return ResponseEntity.notFound().build();
    }

    // 6) Удаление серии
    @Operation(summary = "Удалить серию по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteSeries(@PathVariable Long id) {
        if (seriesRepository.existsById(id)) {
            seriesRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }

    // Метод для маппинга сущности Series в DTO
    private SeriesDto mapToDto(Series series) {
        SeriesDto dto = new SeriesDto();
        dto.setId(series.getId());
        dto.setPrefix(series.getPrefix());
        dto.setStartRange(series.getStartRange());
        dto.setEndRange(series.getEndRange());
        dto.setSuffix(series.getSuffix());
        dto.setDescription(series.getDescription());
        dto.setKindId(series.getKind() != null ? series.getKind().getId() : null);

        for (ServiceCenter sc : series.getServiceCenters()) {
            dto.getServiceCenterIds().add(sc.getId());
        }
        for (Characteristic ch : series.getCharacteristics()) {
            dto.getCharacteristicIds().add(ch.getId());
        }
        for (Advantage ad : series.getAdvantages()) {
            dto.getAdvantageIds().add(ad.getId());
        }
        for (Attribute at : series.getAttributes()) {
            dto.getAttributeIds().add(at.getId());
        }
        for (Boiler bo : series.getBoilers()) {
            dto.getBoilerIds().add(bo.getId());
        }
        for (FileData fd : series.getFiles()) {
            dto.getFileDataIds().add(fd.getId());
        }
        for (Error er : series.getErrors()) {
            dto.getErrorIds().add(er.getId());
        }
        for (BoilerSeriesPassport pt : series.getBoilerSeriesPassports()) {
            dto.getPassportTitleIds().add(pt.getId());
        }

        return dto;
    }
}