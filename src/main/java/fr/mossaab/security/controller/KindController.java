package fr.mossaab.security.controller;
import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Type;

import fr.mossaab.security.repository.KindRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.TypeRepository;
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

@Tag(name = "Виды котлов", description = "API для работы с видами котлов")
@RestController
@SecurityRequirements
@RequiredArgsConstructor
@RequestMapping("/kind")
public class KindController {

    private final KindRepository kindRepository;
    private final TypeRepository typeRepository;
    private final SeriesRepository seriesRepository;

    @Operation(summary = "Поиск вида по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<KindDto> getKindById(@PathVariable Long id) {
        Optional<Kind> kindOptional = kindRepository.findById(id);
        if (kindOptional.isPresent()) {
            Kind kind = kindOptional.get();
            KindDto kindDto = convertToDto(kind);
            return ResponseEntity.ok(kindDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Вывод всех видов")
    @GetMapping("/get-all")
    public ResponseEntity<List<KindDto>> getAllKinds() {
        List<Kind> kinds = kindRepository.findAll();
        List<KindDto> kindDtos = new ArrayList<>();

        for (Kind kind : kinds) {
            kindDtos.add(convertToDto(kind));
        }

        return ResponseEntity.ok(kindDtos);
    }

    @Operation(summary = "Создание нового вида")
    @PostMapping("/add")
    public ResponseEntity<KindDto> createKind(@RequestBody CreateKindDto createKindDto) {
        Kind kind = new Kind();
        kind.setTitle(createKindDto.getTitle());
        kind.setDescription(createKindDto.getDescription());
        kind = kindRepository.save(kind);

        KindDto kindDto = convertToDto(kind);
        return ResponseEntity.status(HttpStatus.CREATED).body(kindDto);
    }

    @Operation(summary = "Обновление полей вида")
    @PutMapping("/update/{id}")
    public ResponseEntity<KindDto> updateKind(
            @PathVariable Long id,
            @RequestBody UpdateKindDto updateKindDto
    ) {
        Optional<Kind> kindOptional = kindRepository.findById(id);
        if (kindOptional.isPresent()) {
            Kind kind = kindOptional.get();

            if (updateKindDto.getTitle() != null) {
                kind.setTitle(updateKindDto.getTitle());
            }
            if (updateKindDto.getDescription() != null) {
                kind.setDescription(updateKindDto.getDescription());
            }
            if (updateKindDto.getTypeId() != null) {
                Optional<Type> typeOptional = typeRepository.findById(updateKindDto.getTypeId());
                if (typeOptional.isPresent()) {
                    kind.setType(typeOptional.get());
                }
            }
            if (updateKindDto.getSeriesId() != null) {
                Optional<Series> seriesOptional = seriesRepository.findById(updateKindDto.getSeriesId());
                if (seriesOptional.isPresent()) {
                    Series series = seriesOptional.get();

                    // Устанавливаем связь с Kind
                    series.setKind(kind);  // Связываем Series с Kind

                    // Если коллекция не была инициализирована, создаем новую
                    if (kind.getSeries() == null) {
                        kind.setSeries(new ArrayList<>());
                    }

                    // Добавляем Series в коллекцию Kind
                    kind.getSeries().add(series);
                }
            }

            // Сохраняем обновленный объект Kind
            kind = kindRepository.save(kind);

            // Конвертируем в DTO и возвращаем ответ
            KindDto kindDto = convertToDto(kind);
            return ResponseEntity.ok(kindDto);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Удаление вида по идентификатору с отвязкой от связанных сущностей")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteKindById(@PathVariable Long id) {
        Optional<Kind> kindOptional = kindRepository.findById(id);
        if (kindOptional.isPresent()) {
            Kind kind = kindOptional.get();

            // Отвязываем связанные сущности
            kind.setType(null);  // Убираем связь с типом
            kind.getSeries().clear();  // Убираем все связи с сериями

            kindRepository.save(kind);  // Сохраняем изменения без удаления

            kindRepository.deleteById(id);  // Удаляем сам объект Kind
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // Helper method to convert Kind to KindDto
    private KindDto convertToDto(Kind kind) {
        List<Long> seriesIds = new ArrayList<>();
        if (kind.getSeries() != null) {
            for (Series series : kind.getSeries()) {
                seriesIds.add(series.getId());
            }
        }
        return new KindDto(
                kind.getId(),
                kind.getTitle(),
                kind.getDescription(),
                kind.getType() != null ? kind.getType().getId() : null,
                seriesIds
        );
    }

    // DTO Classes
    @Data
    @AllArgsConstructor
    public static class KindDto {
        private Long id;
        @Schema(example = "Одноконтурные")
        private String title;
        @Schema(example = "(с закрытой камерой) без трёхходового клапана")
        private String description;
        private Long typeId;
        private List<Long> seriesIds;
    }

    @Data
    public static class CreateKindDto {
        @Schema(example = "Одноконтурные")
        private String title;
        @Schema(example = "(с закрытой камерой) без трёхходового клапана")
        private String description;
    }

    @Data
    public static class UpdateKindDto {
        @Schema(example = "Одноконтурные", nullable = true)
        private String title;
        @Schema(example = "(с закрытой камерой) без трёхходового клапана", nullable = true)
        private String description;
        @Schema(nullable = true)
        private Long typeId;    // ID of the Type to be associated with the Kind
        @Schema(nullable = true)
        private Long seriesId;  // ID of the Series to be added to the Kind's list
    }
}