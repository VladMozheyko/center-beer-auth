package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.ServiceCenter;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.ServiceCenterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Сервисные центры", description = "API для работы с сервисными центрами")
@RestController
@SecurityRequirements()
@RequiredArgsConstructor
@RequestMapping("/service-centers")
public class ServiceCenterController {

    private final ServiceCenterRepository serviceCenterRepository;

    private final SeriesRepository seriesRepository;

    @Operation(summary = "Получить все сервисные центры с полями и списком идентификаторов серий")
    @GetMapping("/get-all-service-centers")
    public ResponseEntity<List<ServiceCenterDto>> getAllServiceCenters() {
        List<ServiceCenter> serviceCenters = serviceCenterRepository.findAll();
        List<ServiceCenterDto> result = new ArrayList<>();

        for (ServiceCenter serviceCenter : serviceCenters) {
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : serviceCenter.getSeriesList()) {
                seriesIds.add(series.getId());
            }
            result.add(new ServiceCenterDto(serviceCenter.getId(), serviceCenter.getTitle(), serviceCenter.getCity(),
                    serviceCenter.getAddress(), serviceCenter.getPhone(), serviceCenter.getServicedEquipment(),
                    serviceCenter.getLatitude(), serviceCenter.getLongitude(), seriesIds));
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Создание сервисного центра без связных объектов")
    @PostMapping("/add-service")
    public ResponseEntity<ServiceCenter> createServiceCenter(@RequestBody ServiceCenterCreateDto dto) {
        ServiceCenter serviceCenter = ServiceCenter.builder()
                .title(dto.getTitle())
                .city(dto.getCity())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .servicedEquipment(dto.getServicedEquipment())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
        return ResponseEntity.ok(serviceCenterRepository.save(serviceCenter));
    }
    @Operation(summary = "Найти все сервисные центры по городу и вывести список серий")
    @GetMapping("/find-all-by-city/{city}")
    public ResponseEntity<List<ServiceCenterWithSeriesDto>> getServiceCentersByCity(@PathVariable String city) {
        List<ServiceCenter> serviceCenters = serviceCenterRepository.findByCity(city);
        List<ServiceCenterWithSeriesDto> result = new ArrayList<>();

        for (ServiceCenter serviceCenter : serviceCenters) {
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : serviceCenter.getSeriesList()) {
                seriesIds.add(series.getId());
            }
            result.add(new ServiceCenterWithSeriesDto(serviceCenter.getId(), serviceCenter.getTitle(), seriesIds));
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Найти сервисный центр по идентификатору и вывести список идентификаторов серий")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ServiceCenterWithSeriesDto> getServiceCenterById(@PathVariable Long id) {
        Optional<ServiceCenter> optionalServiceCenter = serviceCenterRepository.findById(id);
        if (optionalServiceCenter.isPresent()) {
            ServiceCenter serviceCenter = optionalServiceCenter.get();
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : serviceCenter.getSeriesList()) {
                seriesIds.add(series.getId());
            }
            return ResponseEntity.ok(new ServiceCenterWithSeriesDto(serviceCenter.getId(), serviceCenter.getTitle(), seriesIds));
        }
        return ResponseEntity.notFound().build();
    }
    @Operation(summary = "Найти сервисный центр по названию и вывести список идентификаторов серий")
    @GetMapping("/find-by-title/{title}")
    public ResponseEntity<ServiceCenterWithSeriesDto> getServiceCenterByTitle(@PathVariable String title) {
        Optional<ServiceCenter> optionalServiceCenter = serviceCenterRepository.findByTitle(title);
        if (optionalServiceCenter.isPresent()) {
            ServiceCenter serviceCenter = optionalServiceCenter.get();
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : serviceCenter.getSeriesList()) {
                seriesIds.add(series.getId());
            }
            return ResponseEntity.ok(new ServiceCenterWithSeriesDto(serviceCenter.getId(), serviceCenter.getTitle(), seriesIds));
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Удалить сервисный центр по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteServiceCenterById(@PathVariable Long id) {
        if (serviceCenterRepository.existsById(id)) {
            serviceCenterRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Редактировать поля сервисного центра по идентификатору")
    @PatchMapping("/update-by-id/{id}")
    public ResponseEntity<ServiceCenter> updateServiceCenter(@PathVariable Long id,
                                                             @RequestBody ServiceCenterUpdateDto dto) {
        Optional<ServiceCenter> optionalServiceCenter = serviceCenterRepository.findById(id);
        if (optionalServiceCenter.isPresent()) {
            ServiceCenter serviceCenter = optionalServiceCenter.get();
            if (dto.getTitle() != null) serviceCenter.setTitle(dto.getTitle());
            if (dto.getCity() != null) serviceCenter.setCity(dto.getCity());
            if (dto.getAddress() != null) serviceCenter.setAddress(dto.getAddress());
            if (dto.getPhone() != null) serviceCenter.setPhone(dto.getPhone());
            if (dto.getServicedEquipment() != null) serviceCenter.setServicedEquipment(dto.getServicedEquipment());
            if (dto.getLatitude() != null) serviceCenter.setLatitude(dto.getLatitude());
            if (dto.getLongitude() != null) serviceCenter.setLongitude(dto.getLongitude());
            return ResponseEntity.ok(serviceCenterRepository.save(serviceCenter));
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Добавить серию к сервисному центру по идентификатору")
    @PostMapping("/update-series-list/{id}/{seriesId}")
    public ResponseEntity<ServiceCenterWithSeriesDto> addSeriesToServiceCenter(@PathVariable Long id, @PathVariable Long seriesId) {
        Optional<ServiceCenter> optionalServiceCenter = serviceCenterRepository.findById(id);
        Optional<Series> optionalSeries = seriesRepository.findById(seriesId);
        if (optionalServiceCenter.isPresent() && optionalSeries.isPresent()) {
            ServiceCenter serviceCenter = optionalServiceCenter.get();
            Series series = optionalSeries.get();
            serviceCenter.getSeriesList().add(series);
            serviceCenterRepository.save(serviceCenter);

            List<Long> seriesIds = new ArrayList<>();
            for (Series s : serviceCenter.getSeriesList()) {
                seriesIds.add(s.getId());
            }
            return ResponseEntity.ok(new ServiceCenterWithSeriesDto(serviceCenter.getId(), serviceCenter.getTitle(), seriesIds));
        }
        return ResponseEntity.notFound().build();
    }

    @Data
    @AllArgsConstructor
    static class ServiceCenterDto {
        private Long id;
        private String title;
        private String city;
        private String address;
        private String phone;
        private String servicedEquipment;
        private double latitude;
        private double longitude;
        private List<Long> seriesIds;
    }

    @Data
    @AllArgsConstructor
    static class ServiceCenterWithSeriesDto {
        private Long id;
        private String title;
        private List<Long> seriesIds;
    }

    @Data
    @Builder
    static class ServiceCenterCreateDto {
        private String title;
        private String city;
        private String address;
        private String phone;
        private String servicedEquipment;
        private Double latitude;
        private Double longitude;
    }

    @Data
    static class ServiceCenterUpdateDto {
        private String title;
        private String city;
        private String address;
        private String phone;
        private String servicedEquipment;
        private Double latitude;
        private Double longitude;
    }
}
