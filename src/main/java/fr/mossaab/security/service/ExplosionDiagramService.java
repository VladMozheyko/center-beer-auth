package fr.mossaab.security.service;


import fr.mossaab.security.entities.ExplosionDiagram;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.SparePart;
import fr.mossaab.security.repository.ExplosionDiagramRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SparePartRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExplosionDiagramService {

    private final ExplosionDiagramRepository explosionDiagramRepository;
    private final SparePartRepository sparePartRepository;
    private final FileDataRepository fileDataRepository;
    private final StorageService storageService;

    public List<ExplosionDiagramDto> getAllExplosionDiagrams() {
        List<ExplosionDiagramDto> diagramDtos = new ArrayList<>();
        for (ExplosionDiagram diagram : explosionDiagramRepository.findAll()) {
            diagramDtos.add(new ExplosionDiagramDto(diagram));
        }
        return diagramDtos;
    }

    public Optional<ExplosionDiagramDto> getExplosionDiagramById(Long id) {
        return explosionDiagramRepository.findById(id).map(ExplosionDiagramDto::new);
    }

    public boolean deleteExplosionDiagramById(Long id) {
        Optional<ExplosionDiagram> diagramOptional = explosionDiagramRepository.findById(id);
        if (diagramOptional.isPresent()) {
            ExplosionDiagram diagram = diagramOptional.get();

            // Отвязываем запчасти
            for (SparePart sparePart : diagram.getSpareParts()) {
                sparePart.setExplosionDiagram(null);
                sparePartRepository.save(sparePart);
            }

            // Удаляем файл, если он существует
            if (diagram.getFileData() != null) {
                fileDataRepository.delete(diagram.getFileData());
                diagram.setFileData(null); // Отвязываем файл
            }

            // Удаляем диаграмму
            explosionDiagramRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<ExplosionDiagramDto> updateExplosionDiagram(Long id, ExplosionDiagramUpdateDto diagramDto) {
        Optional<ExplosionDiagram> optionalDiagram = explosionDiagramRepository.findById(id);

        if (optionalDiagram.isPresent()) {
            ExplosionDiagram diagram = optionalDiagram.get();

            // Обновление запчастей
            List<SparePart> updatedSpareParts = new ArrayList<>();
            for (Long sparePartId : diagramDto.getSparePartIds()) {
                sparePartRepository.findById(sparePartId).ifPresent(updatedSpareParts::add);
            }
            diagram.setSpareParts(updatedSpareParts);

            // Обновление имени
            if (diagramDto.getName() != null) {
                diagram.setName(diagramDto.getName());
            }

            // Обновление файла
            if (diagramDto.getFileDataId() != null) {
                FileData fileData = new FileData();
                fileData.setId(diagramDto.getFileDataId());
                diagram.setFileData(fileData);
            }

            ExplosionDiagram updatedDiagram = explosionDiagramRepository.save(diagram);
            return Optional.of(new ExplosionDiagramDto(updatedDiagram));
        }
        return Optional.empty();
    }

    public String createExplosionDiagram(String name, MultipartFile image) throws IOException {
        ExplosionDiagram diagram = new ExplosionDiagram();
        diagram.setName(name);

        ExplosionDiagram savedDiagram = explosionDiagramRepository.save(diagram);

        FileData fileData = (FileData) storageService.uploadImageToFileSystem(image, savedDiagram);
        fileData.setExplosionDiagram(savedDiagram);

        fileDataRepository.save(fileData);

        savedDiagram.setFileData(fileData);
        explosionDiagramRepository.save(savedDiagram);

        return "Взрыв-схема создана";
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExplosionDiagramDto {

        private Long id;
        private String name; // Новое поле
        private List<Long> sparePartIds;
        private Long fileDataId;

        public ExplosionDiagramDto(ExplosionDiagram explosionDiagram) {
            this.id = explosionDiagram.getId();
            this.name = explosionDiagram.getName();
            this.sparePartIds = explosionDiagram.getSpareParts().stream().map(SparePart::getId).collect(Collectors.toList());
            this.fileDataId = explosionDiagram.getFileData() != null ? explosionDiagram.getFileData().getId() : null;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExplosionDiagramUpdateDto {
        private String name; // Новое поле
        @Schema(nullable = true)
        private List<Long> sparePartIds;
        @Schema(nullable = true)
        private Long fileDataId;
    }
}