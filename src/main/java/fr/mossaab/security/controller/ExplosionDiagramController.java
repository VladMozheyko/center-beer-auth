package fr.mossaab.security.controller;

import fr.mossaab.security.entities.ExplosionDiagram;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.SparePart;
import fr.mossaab.security.repository.ExplosionDiagramRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SparePartRepository;
import fr.mossaab.security.service.ExplosionDiagramService;
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

    private final ExplosionDiagramService explosionDiagramService;

    @Operation(summary = "Получить все взрыв-схемы")
    @GetMapping("/get-all")
    public List<ExplosionDiagramService.ExplosionDiagramDto> getAllExplosionDiagrams() {
        return explosionDiagramService.getAllExplosionDiagrams();
    }

    @Operation(summary = "Получить взрыв-схему по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ExplosionDiagramService.ExplosionDiagramDto> getExplosionDiagramById(@PathVariable Long id) {
        return explosionDiagramService.getExplosionDiagramById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Удалить взрыв-схему по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteExplosionDiagramById(@PathVariable Long id) {
        if (explosionDiagramService.deleteExplosionDiagramById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Обновить взрыв-схему")
    @PutMapping("/update/{id}")
    public ResponseEntity<ExplosionDiagramService.ExplosionDiagramDto> updateExplosionDiagram(@PathVariable Long id, @RequestBody ExplosionDiagramService.ExplosionDiagramUpdateDto diagramDto) {
        return explosionDiagramService.updateExplosionDiagram(id, diagramDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Создать взрыв-схему")
    @PostMapping(value = "/add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createExplosionDiagram(@RequestPart String name, @RequestPart MultipartFile image) throws IOException {
        return ResponseEntity.ok(explosionDiagramService.createExplosionDiagram(name, image));
    }
}

