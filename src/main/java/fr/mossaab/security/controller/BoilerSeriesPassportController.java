package fr.mossaab.security.controller;

import fr.mossaab.security.entities.BoilerSeriesPassport;
import fr.mossaab.security.service.BoilerSeriesPassportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/boiler-series-passport")
@Tag(name = "Паспорта продукции", description = "API для работы с паспортами серий")
@RequiredArgsConstructor
public class BoilerSeriesPassportController {

    private final BoilerSeriesPassportService boilerSeriesPassportService;

    @GetMapping("/get-all")
    @Operation(summary = "Получить все паспорта продукции")
    public List<BoilerSeriesPassportService.BoilerSeriesPassportDTO> getAllBoilerSeriesPassports() {
        return boilerSeriesPassportService.getAllBoilerSeriesPassports();
    }

    @PostMapping("/add")
    @Operation(summary = "Создать новый паспорт продукции")
    public BoilerSeriesPassport createBoilerSeriesPassport(@RequestPart MultipartFile pdf) throws IOException {
        return boilerSeriesPassportService.createBoilerSeriesPassport(pdf);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Обновить паспорт продукции")
    public BoilerSeriesPassport updateBoilerSeriesPassport(
            @PathVariable Long id,
            @RequestPart(required = false) MultipartFile pdf,
            @RequestPart(required = false) List<Long> seriesIds,
            @RequestParam(required = false) String ruTitle) throws IOException {
        return boilerSeriesPassportService.updateBoilerSeriesPassport(id, pdf, seriesIds, ruTitle);
    }

    @DeleteMapping("/delete-by-id/{id}")
    @Operation(summary = "Удалить паспорт продукции по идентификатору")
    public void deleteBoilerSeriesPassport(@PathVariable Long id) {
        boilerSeriesPassportService.deleteBoilerSeriesPassport(id);
    }

    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Получить паспорт продукции по идентификатору")
    public BoilerSeriesPassportService.BoilerSeriesPassportDTO getBoilerSeriesPassportById(@PathVariable Long id) {
        return boilerSeriesPassportService.getBoilerSeriesPassportById(id);
    }

}
