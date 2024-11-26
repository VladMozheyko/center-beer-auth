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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExplosionDiagramDto {

        private Long id;
        private List<Long> sparePartIds;
        private Long fileDataId;

        public ExplosionDiagramDto(ExplosionDiagram explosionDiagram) {
            this.id = explosionDiagram.getId();
            this.sparePartIds = explosionDiagram.getSpareParts().stream().map(SparePart::getId).collect(Collectors.toList());
            this.fileDataId = explosionDiagram.getFileData() != null ? explosionDiagram.getFileData().getId() : null;
        }
    }
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

    // 3. Получить ExplosionDiagram по наименованию
    @Operation(summary = "Получить взрыв-схему по наименованию")
    @GetMapping("/find-by-name/{name}")
    public ResponseEntity<ExplosionDiagramDto> getExplosionDiagramByName(@PathVariable String name) {
        Optional<ExplosionDiagram> diagram = explosionDiagramRepository.findByName(name);
        return diagram.map(value -> ResponseEntity.ok(new ExplosionDiagramDto(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 4. Удалить ExplosionDiagram по идентификатору
    @Operation(summary = "Удалить взрыв-схему по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteExplosionDiagramById(@PathVariable Long id) {
        if (explosionDiagramRepository.existsById(id)) {
            explosionDiagramRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. Удалить ExplosionDiagram по наименованию
    @Operation(summary = "Удалить взрыв-схему по наименованию")
    @DeleteMapping("/delete-by-name/{name}")
    public ResponseEntity<Void> deleteExplosionDiagramByName(@PathVariable String name) {
        Optional<ExplosionDiagram> diagram = explosionDiagramRepository.findByName(name);
        if (diagram.isPresent()) {
            explosionDiagramRepository.deleteByName(name);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 6. Обновить ExplosionDiagram
    @Operation(summary = "Обновить взрыв-схему")
    @PutMapping("/update/{id}")
    public ResponseEntity<ExplosionDiagramDto> updateExplosionDiagram(@PathVariable Long id, @RequestBody ExplosionDiagramDto diagramDto) {
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
    public ResponseEntity<String> createExplosionDiagram(@RequestBody ExplosionDiagramDto createDto, @RequestPart MultipartFile image) throws IOException {
        ExplosionDiagram diagram = new ExplosionDiagram();
        ExplosionDiagram savedDiagram = explosionDiagramRepository.save(diagram);
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image,savedDiagram);
        fileDataRepository.save(uploadImage);
        return ResponseEntity.ok("Взрыв-схема создана");
    }
}

