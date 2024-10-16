package fr.mossaab.security.controller;

import fr.mossaab.security.dto.response.AdvantageResponse;
import fr.mossaab.security.dto.response.SeriesResponse;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.SeriesRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;
@Tag(name = "Heating System", description = "Контроллер содержит методы связанные с отопительной системой")
@RestController
@RequestMapping("/heatingSystem")
@SecurityRequirements()
@RequiredArgsConstructor
public class HeatingSystemController {
    @Autowired
    private SeriesRepository seriesRepository;
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
