package fr.mossaab.security.controller;

import fr.mossaab.security.entities.ExplosionDiagram;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.SparePart;
import fr.mossaab.security.repository.ExplosionDiagramRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SparePartRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "Взрыв-схемы", description = "API для работы со взрыв-схемами")
@RestController
@RequiredArgsConstructor
@RequestMapping("/explosion-diagram")
public class ExplosionDiagramController {

    private final ExplosionDiagramRepository explosionDiagramRepository;
    private final SparePartRepository sparePartRepository;
    private final StorageService storageService;
    private final FileDataRepository fileDataRepository;

    // 1. Получить все ExplosionDiagram
    @Operation(summary = "Получить все взрыв-схемы")
    @GetMapping("/get-all")
    public List<ExplosionDiagramDto> getAllExplosionDiagrams() {
        List<ExplosionDiagramDto> diagramDtos = new ArrayList<>();
        for (ExplosionDiagram diagram : explosionDiagramRepository.findAll()) {
            diagramDtos.add(new ExplosionDiagramDto(diagram));
        }
        return diagramDtos;
    }

    // 2. Получить ExplosionDiagram по идентификатору
    @Operation(summary = "Получить взрыв-схему по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ExplosionDiagramDto> getExplosionDiagramById(@PathVariable Long id) {
        Optional<ExplosionDiagram> diagram = explosionDiagramRepository.findById(id);
        return diagram.map(value -> ResponseEntity.ok(new ExplosionDiagramDto(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 4. Удалить ExplosionDiagram по идентификатору
    @Operation(summary = "Удалить взрыв-схему по идентификатору с отвязкой от связанных сущностей")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteExplosionDiagramById(@PathVariable Long id) {
        Optional<ExplosionDiagram> diagramOptional = explosionDiagramRepository.findById(id);
        if (diagramOptional.isPresent()) {
            ExplosionDiagram diagram = diagramOptional.get();

            // Отвязываем все связанные запчасти
            for (SparePart sparePart : diagram.getSpareParts()) {
                sparePart.setExplosionDiagram(null);  // Убираем связь с текущей диаграммой
                sparePartRepository.save(sparePart);   // Сохраняем изменения
            }

            // Отвязываем файл, если он существует
            if (diagram.getFileData() != null) {
                diagram.setFileData(null);  // Убираем связь с файлом
                fileDataRepository.save(diagram.getFileData());  // Сохраняем изменения
            }

            // После отвязки удаляем саму диаграмму
            explosionDiagramRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


    // 6. Обновить ExplosionDiagram
    @Operation(summary = "Обновить взрыв-схему")
    @PutMapping("/update/{id}")
    public ResponseEntity<ExplosionDiagramDto> updateExplosionDiagram(@PathVariable Long id, @RequestBody ExplosionDiagramUpdateDto diagramDto) {
        Optional<ExplosionDiagram> optionalDiagram = explosionDiagramRepository.findById(id);

        if (optionalDiagram.isPresent()) {
            ExplosionDiagram diagram = optionalDiagram.get();

            // Обновление связных запчастей
            List<SparePart> updatedSpareParts = new ArrayList<>();
            for (Long sparePartId : diagramDto.getSparePartIds()) {
                Optional<SparePart> optionalSparePart = sparePartRepository.findById(sparePartId);
                optionalSparePart.ifPresent(updatedSpareParts::add);
            }
            diagram.setSpareParts(updatedSpareParts);
            if(diagramDto.getName() != null){
                diagram.setName(diagramDto.getName());
            }
            // Обновление данных файла
            if (diagramDto.getFileDataId() != null) {
                FileData fileData = new FileData();
                fileData.setId(diagramDto.getFileDataId());
                diagram.setFileData(fileData);
            }

            ExplosionDiagram updatedDiagram = explosionDiagramRepository.save(diagram);
            return ResponseEntity.ok(new ExplosionDiagramDto(updatedDiagram));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 9. Создать новый ExplosionDiagram
    @Operation(summary = "Создать взрыв-схему")
    @PostMapping(value = "/add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createExplosionDiagram(@RequestPart String name, @RequestPart MultipartFile image) throws IOException {
        // Создание взрыв-схемы
        ExplosionDiagram diagram = new ExplosionDiagram();
        diagram.setName(name);

        // Сохранение взрыв-схемы для получения ID
        ExplosionDiagram savedDiagram = explosionDiagramRepository.save(diagram);

        // Создание файла данных
        FileData fileData = (FileData) storageService.uploadImageToFileSystem(image, savedDiagram);
        fileData.setExplosionDiagram(savedDiagram); // Установка обратной связи

        // Сохранение файла данных
        fileDataRepository.save(fileData);

        // Установка ссылки в диаграмме и сохранение диаграммы
        savedDiagram.setFileData(fileData);
        explosionDiagramRepository.save(savedDiagram);

        return ResponseEntity.ok("Взрыв-схема создана");
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

