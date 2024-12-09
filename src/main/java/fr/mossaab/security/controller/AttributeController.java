package fr.mossaab.security.controller;

import fr.mossaab.security.service.AttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Аттрибуты", description = "API для работы с аттрибутами")
@RestController
@SecurityRequirements
@RequiredArgsConstructor
@RequestMapping("/attribute")
public class AttributeController {

    private final AttributeService attributeService;

    @Operation(summary = "Поиск атрибута по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<AttributeService.AttributeDto> getAttributeById(@PathVariable Long id) {
        return attributeService.getAttributeById(id);
    }

    @Operation(summary = "Вывод всех атрибутов")
    @GetMapping("/get-all")
    public ResponseEntity<List<AttributeService.AttributeDto>> getAllAttributes() {
        return ResponseEntity.ok(attributeService.getAllAttributes());
    }

    @Operation(summary = "Создание нового атрибута")
    @PostMapping("/add")
    public ResponseEntity<AttributeService.AttributeDto> createAttribute(@RequestBody AttributeService.CreateAttributeDto createAttributeDto) {
        return attributeService.createAttribute(createAttributeDto);
    }

    @Operation(summary = "Обновление атрибута")
    @PutMapping("/update/{id}")
    public ResponseEntity<AttributeService.AttributeDto> updateAttribute(
            @PathVariable Long id,
            @RequestBody AttributeService.UpdateAttributeDto updateAttributeDto) {
        return attributeService.updateAttribute(id, updateAttributeDto);
    }

    @DeleteMapping("/delete-by-id/{id}")
    @Operation(summary = "Удаление атрибута по идентификатору")
    public ResponseEntity<Void> deleteAttribute(@PathVariable Long id) {
        return attributeService.deleteAttribute(id);
    }

}
