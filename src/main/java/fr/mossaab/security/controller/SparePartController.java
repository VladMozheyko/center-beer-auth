package fr.mossaab.security.controller;

import fr.mossaab.security.entities.SparePart;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.ExplosionDiagram;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SparePartRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        public Long fileDataId;
        public Long explosionDiagramId;
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
        }
    }

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
        if (sparePartRepository.existsById(id)) {
            sparePartRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Удалить запчасть по артикулу")
    @DeleteMapping("/delete-by-article/{articleNumber}")
    public ResponseEntity<Void> deleteSparePartByArticle(@PathVariable String articleNumber) {
        Optional<SparePart> sparePart = sparePartRepository.findByArticleNumber(articleNumber);
        if (sparePart.isPresent()) {
            sparePartRepository.delete(sparePart.get());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Удалить запчасть по наименованию")
    @DeleteMapping("/delete-by-name/{name}")
    public ResponseEntity<Void> deleteSparePartByName(@PathVariable String name) {
        List<SparePart> spareParts = sparePartRepository.findByName(name);
        if (!spareParts.isEmpty()) {
            sparePartRepository.deleteAll(spareParts);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Обновить запчасть по идентификатору")
    @PutMapping("/update-by-id/{id}")
    public ResponseEntity<SparePartDto> updateSparePart(@PathVariable Long id, @RequestBody SparePartDto dto) {
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
            // Обновление связных сущностей, если они уже существуют
            if (dto.fileDataId != null) {
                FileData fileData = new FileData();
                fileData.setId(dto.fileDataId);
                sparePart.setFileData(fileData);
            }
            if (dto.explosionDiagramId != null) {
                ExplosionDiagram explosionDiagram = new ExplosionDiagram();
                explosionDiagram.setId(dto.explosionDiagramId);
                sparePart.setExplosionDiagram(explosionDiagram);
            }
            sparePartRepository.save(sparePart);
            return ResponseEntity.ok(new SparePartDto(sparePart));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Создать новую запчасть")
    @PostMapping(value = "add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createSparePart(@RequestBody CreateSparePartDto dto, @RequestPart MultipartFile image) throws IOException {
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

        return ResponseEntity.ok("Запчасть создана");
    }
}
