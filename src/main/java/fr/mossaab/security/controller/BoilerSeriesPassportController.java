package fr.mossaab.security.controller;

import fr.mossaab.security.entities.BoilerSeriesPassport;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.BoilerSeriesPassportRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/boiler-series-passport")
@Tag(name = "Паспорта продукции", description = "API для работы с паспортами серий")
@RequiredArgsConstructor
public class BoilerSeriesPassportController {

    private final BoilerSeriesPassportRepository boilerSeriesPassportRepository;
    private final FileDataRepository fileDataRepository;
    private final SeriesRepository seriesRepository;
    private final StorageService storageService;


    // DTO для вывода данных
    @Data
    @AllArgsConstructor
    public static class BoilerSeriesPassportDTO {
        private Long id;
        private Long fileDataId;
        private List<Long> seriesIds;
    }

    // 1. Метод для получения всех BoilerSeriesPassport с идентификаторами связных сущностей
    @GetMapping("/get-all")
    @Operation(summary = "Получить все паспорта продукции")
    public List<BoilerSeriesPassportDTO> getAllBoilerSeriesPassports() {
        List<BoilerSeriesPassport> passports = boilerSeriesPassportRepository.findAll();
        List<BoilerSeriesPassportDTO> dtos = new ArrayList<>();

        for (BoilerSeriesPassport passport : passports) {
            Long fileDataId = passport.getFile() != null ? passport.getFile().getId() : null;
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : passport.getSeriesList()) {
                seriesIds.add(series.getId());
            }
            dtos.add(new BoilerSeriesPassportDTO(passport.getId(), fileDataId, seriesIds));
        }

        return dtos;
    }

    // 2. Метод для создания нового BoilerSeriesPassport без связных сущностей
    @PostMapping("/add")
    @Operation(summary = "Создать новый паспорт продукции")
    public BoilerSeriesPassport createBoilerSeriesPassport(@RequestBody BoilerSeriesPassport passport,@RequestPart MultipartFile image) throws IOException {
        BoilerSeriesPassport boilerSeriesPassport = boilerSeriesPassportRepository.save(passport);
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image,boilerSeriesPassport);
        fileDataRepository.save(uploadImage);
        return boilerSeriesPassport;
    }

    // 3. Метод для обновления BoilerSeriesPassport
    @PutMapping("/update/{id}")
    @Operation(summary = "Обновить паспорт продукции")
    public BoilerSeriesPassport updateBoilerSeriesPassport(
            @PathVariable Long id,
            @RequestParam(required = false) Long fileDataId,
            @RequestParam(required = false) List<Long> seriesIds) {

        BoilerSeriesPassport passport = boilerSeriesPassportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passport not found"));

        if (fileDataId != null) {
            FileData fileData = fileDataRepository.findById(fileDataId)
                    .orElseThrow(() -> new RuntimeException("FileData not found"));
            passport.setFile(fileData);
        }

        if (seriesIds != null) {
            List<Series> seriesList = new ArrayList<>();
            for (Long seriesId : seriesIds) {
                Series series = seriesRepository.findById(seriesId)
                        .orElseThrow(() -> new RuntimeException("Series not found"));
                seriesList.add(series);
            }
            passport.setSeriesList(seriesList);
        }

        return boilerSeriesPassportRepository.save(passport);
    }

    // 4. Метод для удаления BoilerSeriesPassport по идентификатору
    @DeleteMapping("/delete-by-id/{id}")
    @Operation(summary = "Удалить паспорт продукции по идентификатору")
    public void deleteBoilerSeriesPassport(@PathVariable Long id) {
        boilerSeriesPassportRepository.deleteById(id);
    }

    // 5. Метод для поиска по идентификатору и вывода идентификаторов связных сущностей
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Получить паспорт продукции по идентификатору")
    public BoilerSeriesPassportDTO getBoilerSeriesPassportById(@PathVariable Long id) {
        BoilerSeriesPassport passport = boilerSeriesPassportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passport not found"));

        Long fileDataId = passport.getFile() != null ? passport.getFile().getId() : null;
        List<Long> seriesIds = new ArrayList<>();
        for (Series series : passport.getSeriesList()) {
            seriesIds.add(series.getId());
        }

        return new BoilerSeriesPassportDTO(passport.getId(), fileDataId, seriesIds);
    }
}