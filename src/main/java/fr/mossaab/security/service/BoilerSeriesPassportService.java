package fr.mossaab.security.service;

import fr.mossaab.security.entities.BoilerSeriesPassport;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.BoilerSeriesPassportRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.SeriesRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoilerSeriesPassportService {

    private final BoilerSeriesPassportRepository boilerSeriesPassportRepository;
    private final FileDataRepository fileDataRepository;
    private final SeriesRepository seriesRepository;
    private final StorageService storageService;

    // 1. Метод для получения всех BoilerSeriesPassport с идентификаторами связных сущностей
    public List<BoilerSeriesPassportDTO> getAllBoilerSeriesPassports() {
        List<BoilerSeriesPassport> passports = boilerSeriesPassportRepository.findAll();
        List<BoilerSeriesPassportDTO> dtos = new ArrayList<>();

        for (BoilerSeriesPassport passport : passports) {
            Long fileDataId = passport.getFile() != null ? passport.getFile().getId() : null;
            List<Long> seriesIds = new ArrayList<>();
            for (Series series : passport.getSeriesList()) {
                seriesIds.add(series.getId());
            }
            dtos.add(new BoilerSeriesPassportDTO(passport.getId(), passport.getRuTitle(), fileDataId, seriesIds));
        }

        return dtos;
    }

    // 2. Метод для создания нового BoilerSeriesPassport без связных сущностей
    public BoilerSeriesPassport createBoilerSeriesPassport(MultipartFile pdf) throws IOException {
        BoilerSeriesPassport passport = new BoilerSeriesPassport();
        BoilerSeriesPassport boilerSeriesPassport = boilerSeriesPassportRepository.save(passport);
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(pdf, boilerSeriesPassport);
        fileDataRepository.save(uploadImage);
        return boilerSeriesPassport;
    }

    // 3. Метод для обновления BoilerSeriesPassport
    public BoilerSeriesPassport updateBoilerSeriesPassport(
            Long id,
            MultipartFile pdf,
            List<Long> seriesIds,
            String ruTitle) throws IOException {

        BoilerSeriesPassport passport = boilerSeriesPassportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passport not found"));

        if (ruTitle != null) {
            passport.setRuTitle(ruTitle); // Обновляем ruTitle
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

        if (pdf != null) {
            FileData currentFileData = passport.getFile();
            if (currentFileData != null) {
                fileDataRepository.delete(currentFileData);
            }
            FileData newFileData = (FileData) storageService.uploadImageToFileSystem(pdf, passport);
            fileDataRepository.save(newFileData);
            passport.setFile(newFileData);
        }
        return boilerSeriesPassportRepository.save(passport);
    }

    // 4. Метод для удаления BoilerSeriesPassport по идентификатору
    public void deleteBoilerSeriesPassport(Long id) {
        BoilerSeriesPassport passport = boilerSeriesPassportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passport not found"));

        // Отвязываем связанные сущности Series
        for (Series series : passport.getSeriesList()) {
            series.getBoilerSeriesPassports().remove(passport); // Убираем связь у Series
        }

        // Сохраняем изменения в сущностях Series
        for (Series series : passport.getSeriesList()) {
            seriesRepository.save(series);
        }

        // Удаляем файл, если он есть
        if (passport.getFile() != null) {
            fileDataRepository.delete(passport.getFile());
        }

        // Удаляем паспорт
        boilerSeriesPassportRepository.delete(passport);
    }

    // 5. Метод для поиска по идентификатору и вывода идентификаторов связных сущностей
    public BoilerSeriesPassportDTO getBoilerSeriesPassportById(Long id) {
        BoilerSeriesPassport passport = boilerSeriesPassportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passport not found"));

        Long fileDataId = passport.getFile() != null ? passport.getFile().getId() : null;
        List<Long> seriesIds = new ArrayList<>();
        for (Series series : passport.getSeriesList()) {
            seriesIds.add(series.getId());
        }

        return new BoilerSeriesPassportDTO(passport.getId(), passport.getRuTitle(), fileDataId, seriesIds);
    }

    // DTO для вывода данных
    @RequiredArgsConstructor
    @Data
    @AllArgsConstructor
    public static class BoilerSeriesPassportDTO {
        private Long id;
        private String ruTitle;
        private Long fileDataId;
        private List<Long> seriesIds;
    }
}
