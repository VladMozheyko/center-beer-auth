package fr.mossaab.security.controller;

import fr.mossaab.security.service.AdvantageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Преимущества", description = "API для работы с преимуществами")
@RestController
@RequiredArgsConstructor
@RequestMapping("/advantage")
public class AdvantageController {

    private final AdvantageService advantageService;

    @Operation(summary = "Поиск преимущества по ID")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<AdvantageService.AdvantageDTO> getAdvantageById(@PathVariable Long id) {
        return advantageService.getAdvantageById(id);
    }

    @Operation(summary = "Вывод всех преимуществ")
    @GetMapping("get-all")
    public List<AdvantageService.AdvantageDTO> getAllAdvantages() {
        return advantageService.getAllAdvantages();
    }

    @Operation(summary = "Удаление преимущества по ID")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteAdvantageById(@PathVariable Long id) {
        return advantageService.deleteAdvantageById(id);
    }

    @Operation(summary = "Создание преимущества")
    @PostMapping("/add")
    public ResponseEntity<AdvantageService.AdvantageDTO> createAdvantage(
            @RequestPart AdvantageService.AdvantageCreateDTO advantageCreateDTO,
            @RequestPart MultipartFile image) throws IOException {
        return advantageService.createAdvantage(advantageCreateDTO, image);
    }

    @Operation(summary = "Обновление преимущества")
    @PatchMapping("/update/{id}")
    public ResponseEntity<AdvantageService.AdvantageDTO> updateAdvantage(
            @PathVariable Long id,
            @RequestBody AdvantageService.AdvantageUpdateDTO advantageUpdateDTO,
            @RequestParam(required = false) List<Long> seriesIds,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        return advantageService.updateAdvantage(id, advantageUpdateDTO, seriesIds, image);
    }
}

