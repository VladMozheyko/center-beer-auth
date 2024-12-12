package fr.mossaab.security.controller;

import fr.mossaab.security.entities.ExplosionDiagram;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.SparePart;
import fr.mossaab.security.repository.BoilerRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SparePartRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import fr.mossaab.security.entities.Boiler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Запчасти", description = "API для работы с запчастями")
@RestController
@RequiredArgsConstructor
@RequestMapping("/spare-part")
public class SparePartController {

    private final SparePartRepository sparePartRepository;
    private final StorageService storageService;
    private final FileDataRepository fileDataRepository;
    private final BoilerRepository boilerRepository;

    @Operation(summary = "Получить все запчасти с идентификаторами связных сущностей")
    @GetMapping("get-all")
    public List<SparePartDto> getAllSpareParts() {
        List<SparePartDto> sparePartDtos = new ArrayList<>();
        for (SparePart sparePart : sparePartRepository.findAll()) {
            sparePartDtos.add(new SparePartDto(sparePart));
        }
        return sparePartDtos;
    }

    @Operation(summary = "Получить запчасть по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<SparePartDto> getSparePartById(@PathVariable Long id) {
        Optional<SparePart> sparePart = sparePartRepository.findById(id);
        return sparePart.map(part -> ResponseEntity.ok(new SparePartDto(part)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить запчасть по артикулу")
    @GetMapping("/find-by-article/{articleNumber}")
    public ResponseEntity<SparePartDto> getSparePartByArticle(@PathVariable String articleNumber) {
        Optional<SparePart> sparePart = sparePartRepository.findByArticleNumber(articleNumber);
        return sparePart.map(part -> ResponseEntity.ok(new SparePartDto(part)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить запчасть по наименованию")
    @GetMapping("/find-by-name/{name}")
    public List<SparePartDto> getSparePartsByName(@PathVariable String name) {
        List<SparePartDto> sparePartDtos = new ArrayList<>();
        for (SparePart sparePart : sparePartRepository.findByName(name)) {
            sparePartDtos.add(new SparePartDto(sparePart));
        }
        return sparePartDtos;
    }

    @Operation(summary = "Удалить запчасть по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteSparePartById(@PathVariable Long id) {
        // Проверяем, существует ли запчасть
        SparePart sparePart = sparePartRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запчасть с ID " + id + " не найдена."));

        // Удаляем связь с сущностью `FileData`
        if (sparePart.getFileData() != null) {
            fileDataRepository.delete(sparePart.getFileData());
            sparePart.setFileData(null);
        }

        if (!sparePart.getBoilers().isEmpty()) {
            for (Boiler boiler : sparePart.getBoilers()) {
                boiler.getSpareParts().remove(sparePart);
            }
            sparePart.getBoilers().clear();
        }

        // Удаляем связь с сущностью `ExplosionDiagram`
        if (sparePart.getExplosionDiagram() != null) {
            sparePart.setExplosionDiagram(null);
        }

        // После очистки зависимостей удаляем саму запчасть
        sparePartRepository.delete(sparePart);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Обновить запчасть по идентификатору")
    @PutMapping("/update-by-id/{id}")
    public ResponseEntity<SparePartDto> updateSparePart(@PathVariable Long id, @RequestBody UpdateSparePartDto dto,
                                                        @RequestParam(required = false) MultipartFile image) throws IOException {
        Optional<SparePart> sparePartOpt = sparePartRepository.findById(id);
        if (sparePartOpt.isPresent()) {
            SparePart sparePart = sparePartOpt.get();
            sparePart.setArticleNumber(dto.articleNumber);
            sparePart.setName(dto.name);
            sparePart.setAscPriceYuan(dto.ascPriceYuan);
            sparePart.setWholesalePriceYuan(dto.wholesalePriceYuan);
            sparePart.setRetailPriceYuan(dto.retailPriceYuan);
            sparePart.setAscPriceRub(dto.ascPriceRub);
            sparePart.setWholesalePriceRub(dto.wholesalePriceRub);
            sparePart.setRetailPriceRub(dto.retailPriceRub);

            if (dto.boilerIds != null && !dto.boilerIds.isEmpty()) {
                List<Boiler> boilers = boilerRepository.findAllById(dto.boilerIds);
                sparePart.setBoilers(boilers);
            }

            if (dto.explosionDiagramId != null) {
                ExplosionDiagram explosionDiagram = new ExplosionDiagram();
                explosionDiagram.setId(dto.explosionDiagramId);
                sparePart.setExplosionDiagram(explosionDiagram);
            }
            if (image != null) {
                FileData currentFileData = sparePart.getFileData();
                if (currentFileData != null) {
                    // Удаление записи из базы данных
                    fileDataRepository.delete(currentFileData);
                }
                // Загрузка нового изображения и привязка к Advantage
                FileData newFileData = (FileData) storageService.uploadImageToFileSystem(image, sparePart);
                fileDataRepository.save(newFileData);
                sparePart.setFileData(newFileData);
            }
            sparePartRepository.save(sparePart);
            return ResponseEntity.ok(new SparePartDto(sparePart));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Создать новую запчасть")
    @PostMapping(value = "add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createSparePart(@RequestPart CreateSparePartDto dto, @RequestPart MultipartFile image) throws IOException {
        SparePart sparePart = new SparePart();
        sparePart.setArticleNumber(dto.articleNumber);
        sparePart.setName(dto.name);
        sparePart.setAscPriceYuan(dto.ascPriceYuan);
        sparePart.setWholesalePriceYuan(dto.wholesalePriceYuan);
        sparePart.setRetailPriceYuan(dto.retailPriceYuan);
        sparePart.setAscPriceRub(dto.ascPriceRub);
        sparePart.setWholesalePriceRub(dto.wholesalePriceRub);
        sparePart.setRetailPriceRub(dto.retailPriceRub);
        SparePart savedSparePart = sparePartRepository.save(sparePart);
        // Загрузка и сохранение изображения
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image, savedSparePart);
        fileDataRepository.save(uploadImage);
        sparePart.setFileData(uploadImage);
        sparePartRepository.save(sparePart);

        return ResponseEntity.ok("Запчасть создана");
    }

    // DTO для SparePart с идентификаторами связных сущностей
    @Data
    public static class CreateSparePartDto {
        @Schema(example = "AA0101014")
        public String articleNumber;
        @Schema(example = "Электронная плата")
        public String name;
        @Schema(example = "447")
        public BigDecimal ascPriceYuan;
        @Schema(example = "469.35")
        public BigDecimal wholesalePriceYuan;
        @Schema(example = "603.45")
        public BigDecimal retailPriceYuan;
        @Schema(example = "5900.40")
        public BigDecimal ascPriceRub;
        @Schema(example = "6195.42")
        public BigDecimal wholesalePriceRub;
        @Schema(example = "7965.54")
        public BigDecimal retailPriceRub;
    }

    @Data
    public static class UpdateSparePartDto {
        @Schema(example = "AA0101014", nullable = true)
        public String articleNumber;
        @Schema(example = "Электронная плата", nullable = true)
        public String name;
        @Schema(example = "447", nullable = true)
        public BigDecimal ascPriceYuan;
        @Schema(example = "469.35", nullable = true)
        public BigDecimal wholesalePriceYuan;
        @Schema(example = "603.45", nullable = true)
        public BigDecimal retailPriceYuan;
        @Schema(example = "5900.40", nullable = true)
        public BigDecimal ascPriceRub;
        @Schema(example = "6195.42", nullable = true)
        public BigDecimal wholesalePriceRub;
        @Schema(example = "7965.54", nullable = true)
        public BigDecimal retailPriceRub;
        @Schema(nullable = true)
        public Long explosionDiagramId;
        @Schema(nullable = true)
        private List<Long> boilerIds;
    }

    public static class SparePartDto {
        public Long id;
        @Schema(example = "AA0101014")
        public String articleNumber;
        @Schema(example = "Электронная плата")
        public String name;
        @Schema(example = "447")
        public BigDecimal ascPriceYuan;
        @Schema(example = "469.35")
        public BigDecimal wholesalePriceYuan;
        @Schema(example = "603.45")
        public BigDecimal retailPriceYuan;
        @Schema(example = "5900.40")
        public BigDecimal ascPriceRub;
        @Schema(example = "6195.42")
        public BigDecimal wholesalePriceRub;
        @Schema(example = "7965.54")
        public BigDecimal retailPriceRub;
        public Long fileDataId;
        public Long explosionDiagramId;
        private List<Long> boilerIds;
        // Конструктор для создания DTO из сущности SparePart
        public SparePartDto(SparePart sparePart) {
            this.id = sparePart.getId();
            this.articleNumber = sparePart.getArticleNumber();
            this.name = sparePart.getName();
            this.ascPriceYuan = sparePart.getAscPriceYuan();
            this.wholesalePriceYuan = sparePart.getWholesalePriceYuan();
            this.retailPriceYuan = sparePart.getRetailPriceYuan();
            this.ascPriceRub = sparePart.getAscPriceRub();
            this.wholesalePriceRub = sparePart.getWholesalePriceRub();
            this.retailPriceRub = sparePart.getRetailPriceRub();
            this.fileDataId = sparePart.getFileData() != null ? sparePart.getFileData().getId() : null;
            this.explosionDiagramId = sparePart.getExplosionDiagram() != null ? sparePart.getExplosionDiagram().getId() : null;
            this.boilerIds = sparePart.getBoilers().stream().map(Boiler::getId).toList();
        }
    }
}
