package fr.mossaab.security.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.mossaab.security.entities.Advantage;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.enums.CategoryOfAdvantage;
import fr.mossaab.security.repository.AdvantageRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Преимущества", description = "API для работы с преимуществами")
@RestController
@RequiredArgsConstructor
@RequestMapping("/advantage")
public class AdvantageController {

    private final AdvantageRepository advantageRepository;
    private final SeriesRepository seriesRepository;
    private final FileDataRepository fileDataRepository;
    private final StorageService storageService;

    // 1. Get an advantage by ID with related entities
    @Operation(summary = "Поиск преимущества по ID", description = "Найти преимущество по ID и показать его поля и идентификаторы связных сущностей")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<AdvantageDTO> getAdvantageById(@PathVariable Long id) {
        Optional<Advantage> advantageOptional = advantageRepository.findById(id);
        if (advantageOptional.isPresent()) {
            return ResponseEntity.ok(new AdvantageDTO(advantageOptional.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 2. Get all advantages with related entities
    @Operation(summary = "Вывод всех преимуществ", description = "Вывести все преимущества и их поля и идентификаторы связных сущностей")
    @GetMapping("get-all")
    public List<AdvantageDTO> getAllAdvantages() {
        List<AdvantageDTO> advantageDTOs = new ArrayList<>();
        List<Advantage> advantages = advantageRepository.findAll();
        for (Advantage advantage : advantages) {
            advantageDTOs.add(new AdvantageDTO(advantage));
        }
        return advantageDTOs;
    }

    // 3. Delete an advantage by ID
    @Operation(summary = "Удаление преимущества по ID", description = "Удалить преимущество по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteAdvantageById(@PathVariable Long id) {
        if (advantageRepository.existsById(id)) {
            advantageRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 4. Create a new advantage
    @Operation(summary = "Создание преимущества", description = "Создать новое преимущество на основе полей и загрузить изображение")
    @PostMapping("/add")
    public ResponseEntity<AdvantageDTO> createAdvantage(
            @RequestPart AdvantageCreateDTO advantageCreateDTO,
            @RequestPart MultipartFile image) throws IOException {

        Advantage advantage = new Advantage();
        advantage.setTitle(advantageCreateDTO.getTitle());
        advantage.setCategory(advantageCreateDTO.getCategory());
        advantage = advantageRepository.save(advantage);

        // Загрузка изображения и связывание с преимуществом
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image, advantage);
        fileDataRepository.save(uploadImage);
        advantage.setFileData(uploadImage);

        // Сохранение обновленного преимущества с привязкой к FileData
        advantage = advantageRepository.save(advantage);

        return new ResponseEntity<>(new AdvantageDTO(advantage), HttpStatus.CREATED);
    }

    // 5. Update an advantage with fields and related entities
    @Operation(summary = "Обновление преимущества", description = "Обновить преимущество на основе полей и идентификаторов связных сущностей")
    @PatchMapping("/update/{id}")
    public ResponseEntity<AdvantageDTO> updateAdvantage(
            @PathVariable Long id,
            @RequestBody AdvantageUpdateDTO advantageUpdateDTO,
            @RequestParam(required = false) List<Long> seriesIds,
            @RequestParam(required = false) MultipartFile image) throws IOException {

        Optional<Advantage> advantageOptional = advantageRepository.findById(id);
        if (advantageOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Advantage advantage = advantageOptional.get();

        // Update fields if provided
        if (advantageUpdateDTO.getTitle() != null) advantage.setTitle(advantageUpdateDTO.getTitle());
        if (advantageUpdateDTO.getCategory() != null) advantage.setCategory(advantageUpdateDTO.getCategory());

        // Update Series if provided
        if (seriesIds != null) {
            List<Series> seriesList = new ArrayList<>();
            for (Long seriesId : seriesIds) {
                Optional<Series> seriesOptional = seriesRepository.findById(seriesId);
                seriesOptional.ifPresent(seriesList::add);
            }
            advantage.setSeries(seriesList);
        }
        // Удаление предыдущего изображения, если передано новое
        if (image != null) {
            FileData currentFileData = advantage.getFileData();
            if (currentFileData != null) {
                // Удаление записи из базы данных
                fileDataRepository.delete(currentFileData);
            }
            // Загрузка нового изображения и привязка к Advantage
            FileData newFileData = (FileData) storageService.uploadImageToFileSystem(image, advantage);
            fileDataRepository.save(newFileData);
            advantage.setFileData(newFileData);
        }

        // Сохранение обновленного преимущества с привязкой к FileData
        advantage = advantageRepository.save(advantage);
        return ResponseEntity.ok(new AdvantageDTO(advantage));
    }

    @Getter
    @Setter
    public static class AdvantageCreateDTO {
        @Schema(example = "Режим работы с теплыми полами")
        private String title;
        @Schema(example = "COMFORT")
        private CategoryOfAdvantage category;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL) // Исключает null-поля при сериализации
    public static class AdvantageUpdateDTO {
        @Schema(example = "Режим работы с теплыми полами", nullable = true)
        private String title;

        @Schema(example = "COMFORT", nullable = true)
        private CategoryOfAdvantage category;

        @Schema(description = "Список идентификаторов связанных Series", nullable = true)
        private List<Long> seriesIds = new ArrayList<>();
    }

    // DTO for Advantage
    @Getter
    @Setter
    public static class AdvantageDTO {
        private Long id;
        @Schema(example = "Режим работы с теплыми полами")
        private String title;
        @Schema(example = "COMFORT")
        private CategoryOfAdvantage category;
        private List<Long> seriesIds = new ArrayList<>();
        private Long fileDataId;

        // Constructor to map Advantage entity to DTO
        public AdvantageDTO(Advantage advantage) {
            this.id = advantage.getId();
            this.title = advantage.getTitle();
            this.category = advantage.getCategory();
            this.fileDataId = advantage.getFileData() != null ? advantage.getFileData().getId() : null;

            for (Series s : advantage.getSeries()) {
                this.seriesIds.add(s.getId());
            }
        }
    }
}

