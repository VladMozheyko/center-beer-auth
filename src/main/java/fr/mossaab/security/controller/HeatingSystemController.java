package fr.mossaab.security.controller;

import fr.mossaab.security.dto.response.AdvantageResponse;
import fr.mossaab.security.dto.response.SeriesResponse;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.service.impl.StorageAdvantageService;
import fr.mossaab.security.service.impl.StorageSeriesService;
import fr.mossaab.security.service.impl.StorageTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
@Tag(name = "Heating System", description = "Контроллер содержит методы связанные с отопительной системой")
@RestController
@RequestMapping("/heatingSystem")
@SecurityRequirements()
@RequiredArgsConstructor
public class HeatingSystemController {
    private final StorageSeriesService storageSeriesService;
    private final StorageTypeService storageTypeService;
    private final StorageAdvantageService storageAdvantageService;
    @Autowired
    private SeriesRepository seriesRepository;
    @Operation(summary = "Загрузка изображения из файловой системы для типов", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для типов.")
    @GetMapping("/fileSystemTypes/{fileName}")
    public ResponseEntity<?> downloadImageTypesFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageTypeService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Загрузка изображения из файловой системы для преимуществ", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для преимуществ.")
    @GetMapping("/fileSystemAdvantages/{fileName}")
    public ResponseEntity<?> downloadImageAdvantagesFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageAdvantageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Загрузка изображения из файловой системы для серии", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы для серии.")
    @GetMapping("/fileSystemSeries/{fileName}")
    public ResponseEntity<?> downloadImageSeriesFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageSeriesService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }

    @GetMapping("/allSeries")
    public ResponseEntity<List<SeriesResponse>> getAllSeries() {
        List<Series> seriesList = seriesRepository.findAll();
        List<SeriesResponse> seriesResponses = seriesList.stream()
                .map(series -> new SeriesResponse(
                        series.getTitle(),
                        series.getDescription(),
                        series.getAdvantages().stream()
                                .map(advantage -> new AdvantageResponse(advantage.getTitle(),
                                        advantage.getCategory(),
                                        "http://31.129.102.70:8080/heatingSystem/fileSystemAdvantages/"+ advantage.getIconPath()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(seriesResponses);
    }
}
