package fr.mossaab.security.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.mossaab.security.entities.Advantage;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.enums.CategoryOfAdvantage;
import fr.mossaab.security.repository.AdvantageRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SeriesRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdvantageService {

    private final AdvantageRepository advantageRepository;
    private final SeriesRepository seriesRepository;
    private final FileDataRepository fileDataRepository;
    private final StorageService storageService;

    public ResponseEntity<AdvantageDTO> getAdvantageById(Long id) {
        Optional<Advantage> advantageOptional = advantageRepository.findById(id);
        if (advantageOptional.isPresent()) {
            return ResponseEntity.ok(new AdvantageDTO(advantageOptional.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public List<AdvantageDTO> getAllAdvantages() {
        List<AdvantageDTO> advantageDTOs = new ArrayList<>();
        List<Advantage> advantages = advantageRepository.findAll();
        for (Advantage advantage : advantages) {
            advantageDTOs.add(new AdvantageDTO(advantage));
        }
        return advantageDTOs;
    }

    public ResponseEntity<Void> deleteAdvantageById(Long id) {
        Optional<Advantage> advantageOptional = advantageRepository.findById(id);
        if (advantageOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Advantage advantage = advantageOptional.get();

        if (!advantage.getSeries().isEmpty()) {
            advantage.getSeries().clear();
        }

        FileData fileData = advantage.getFileData();
        if (fileData != null) {
            fileDataRepository.delete(fileData);
        }

        advantageRepository.delete(advantage);

        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<AdvantageDTO> createAdvantage(AdvantageCreateDTO advantageCreateDTO, MultipartFile image) throws IOException {
        Advantage advantage = new Advantage();
        advantage.setTitle(advantageCreateDTO.getTitle());
        advantage.setCategory(advantageCreateDTO.getCategory());
        advantage = advantageRepository.save(advantage);

        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image, advantage);
        fileDataRepository.save(uploadImage);
        advantage.setFileData(uploadImage);

        advantage = advantageRepository.save(advantage);

        return new ResponseEntity<>(new AdvantageDTO(advantage), HttpStatus.CREATED);
    }

    public ResponseEntity<AdvantageDTO> updateAdvantage(Long id, AdvantageUpdateDTO advantageUpdateDTO, List<Long> seriesIds, MultipartFile image) throws IOException {
        Optional<Advantage> advantageOptional = advantageRepository.findById(id);
        if (advantageOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Advantage advantage = advantageOptional.get();

        if (advantageUpdateDTO.getTitle() != null) advantage.setTitle(advantageUpdateDTO.getTitle());
        if (advantageUpdateDTO.getCategory() != null) advantage.setCategory(advantageUpdateDTO.getCategory());

        if (seriesIds != null) {
            List<Series> seriesList = new ArrayList<>();
            for (Long seriesId : seriesIds) {
                seriesRepository.findById(seriesId).ifPresent(seriesList::add);
            }
            advantage.setSeries(seriesList);
        }

        if (image != null) {
            FileData currentFileData = advantage.getFileData();
            if (currentFileData != null) {
                fileDataRepository.delete(currentFileData);
            }

            FileData newFileData = (FileData) storageService.uploadImageToFileSystem(image, advantage);
            fileDataRepository.save(newFileData);
            advantage.setFileData(newFileData);
        }

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
