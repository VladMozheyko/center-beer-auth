package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.PassportCategory;
import fr.mossaab.security.entities.PassportFileData;
import fr.mossaab.security.entities.PassportTitle;
import fr.mossaab.security.entities.SeriesTitle;
import fr.mossaab.security.repository.PassportCategoryRepository;
import fr.mossaab.security.repository.PassportFileDataRepository;
import fr.mossaab.security.repository.PassportTitleRepository;
import fr.mossaab.security.service.impl.PassportService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/passport")
@RequiredArgsConstructor
public class PassportController {

    private final PassportCategoryRepository passportCategoryRepository;
    private final PassportTitleRepository passportTitleRepository;
    private final PassportFileDataRepository passportFileDataRepository;
    private final PassportService passportService;
    @GetMapping("/categories")
    public List<CategoryWithTitlesDTO> getAllCategoriesWithTitlesAndFiles() {
        List<CategoryWithTitlesDTO> categoryWithTitlesList = new ArrayList<>();

        List<PassportCategory> categories = passportCategoryRepository.findAll();
        for (PassportCategory category : categories) {
            List<PassportTitleWithFilesDTO> titleWithFilesList = new ArrayList<>();
            List<PassportTitle> titles = passportTitleRepository.findAllByCategory(category);
            for (PassportTitle title : titles) {
                List<PassportFileData> fileDataList = passportFileDataRepository.findByPassportTitle(title);
                // Используем цикл для добавления хоста к каждому имени файла
                List<String> filePaths = new ArrayList<>();
                for (PassportFileData fileData : fileDataList) {
                    String filePath = "http://31.129.102.70:8080/passport/image/" + fileData.getName();
                    filePaths.add(filePath);
                }
                titleWithFilesList.add(new PassportTitleWithFilesDTO(title.getTitle(), title.getRuTitle(), filePaths));
            }

            List<SeriesTitleDTO> seriesTitleDTO = new ArrayList<>();
            List<ErrorDTO> errorDTOList = new ArrayList<>();
            for (SeriesTitle seriesTitle : category.getSeriesTitles()) {
                for (Error error : seriesTitle.getErrors()) {
                    errorDTOList.add(new ErrorDTO(error.getCode(), error.getCause(), error.getDescription()));
                }
                seriesTitleDTO.add(new SeriesTitleDTO(seriesTitle.getTitle(),errorDTOList));
            }
            categoryWithTitlesList.add(new CategoryWithTitlesDTO(category.getTitle(),category.getRuTitle(), titleWithFilesList, seriesTitleDTO));
        }

        return categoryWithTitlesList;
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<?> getImage(@PathVariable String fileName) throws IOException {
        byte[] imageData = passportService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }

    // Класс-оболочка для имени файла
    static class FileNameWrapper {
        private String fileName;

        // Геттер и сеттер для имени файла
        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
    @Setter
    @Getter
    // DTO class to hold category name and its titles
    static class CategoryWithTitlesDTO {
        private String categoryName;
        private String ruCategoryName;
        private List<PassportTitleWithFilesDTO> titles;
        private List<SeriesTitleDTO> seriesTitleDTOS;
        public CategoryWithTitlesDTO(String categoryName,String ruCategoryName, List<PassportTitleWithFilesDTO> titles, List<SeriesTitleDTO> seriesTitleDTOS) {
            this.categoryName = categoryName;
            this.ruCategoryName = ruCategoryName;
            this.titles = titles;
            this.seriesTitleDTOS = seriesTitleDTOS;
        }

        // getters and setters
    }
    @Setter
    @Getter
    // DTO class to hold title name and its files
    static class PassportTitleWithFilesDTO {
        private String titleName;
        private String ruTitleName;
        private List<String> files;

        public PassportTitleWithFilesDTO(String titleName,String ruTitleName, List<String> files) {
            this.titleName = titleName;
            this.ruTitleName = ruTitleName;
            this.files = files;
        }

        // getters and setters
    }
    @Setter
    @Getter
    static class SeriesTitleDTO {
        private String seriesTitle;
        private List<ErrorDTO> errors;

        public SeriesTitleDTO(String seriesTitle, List<ErrorDTO> errors) {
            this.seriesTitle = seriesTitle;
            this.errors = errors;
        }
    }
    @Setter
    @Getter
    static class ErrorDTO {
        private String code;
        private String cause;
        private String description;

        public ErrorDTO(String code, String cause, String description) {
            this.code = code;
            this.cause = cause;
            this.description = description;
        }
    }
}
