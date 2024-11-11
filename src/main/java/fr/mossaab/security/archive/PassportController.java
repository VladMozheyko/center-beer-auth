//package fr.mossaab.security.controller;
//
//
//import fr.mossaab.security.entities.*;
//import fr.mossaab.security.entities.Error;
//import fr.mossaab.security.repository.FileDataRepository;
//import fr.mossaab.security.repository.PassportCategoryRepository;
//import fr.mossaab.security.repository.PassportTitleRepository;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Tag(name = "Паспорта", description = "Контроллер предоставляет методы контекста паспортов продукции")
//@RestController
//@RequestMapping("/passport")
//@RequiredArgsConstructor
//public class PassportController {
//    private final PassportCategoryRepository passportCategoryRepository;
//    private final PassportTitleRepository passportTitleRepository;
//    private final FileDataRepository passportFileDataRepository;
//    //private final PassportService passportService;
//    @GetMapping("/categories")
//    public List<CategoryWithTitlesDTO> getAllCategoriesWithTitlesAndFiles() {
//        List<CategoryWithTitlesDTO> categoryWithTitlesList = new ArrayList<>();
//
//        List<PassportCategory> categories = passportCategoryRepository.findAll();
//        for (PassportCategory category : categories) {
//            List<PassportTitleWithFilesDTO> titleWithFilesList = new ArrayList<>();
//            List<PassportTitle> titles = passportTitleRepository.findAllByCategory(category);
//            for (PassportTitle title : titles) {
//                FileData fileData = passportFileDataRepository.findByName(title.getTitle()+".pdf").orElse(null);
//                // Используем цикл для добавления хоста к каждому имени файла
//                String filePaths = "http://31.129.102.70:8080/user/fileSystemPdf/" + fileData.getName();
//                titleWithFilesList.add(new PassportTitleWithFilesDTO(title.getTitle(), title.getRuTitle(), filePaths));
//            }
//
//            List<SeriesTitleDTO> seriesTitleDTO = new ArrayList<>();
//            for (SeriesTitle seriesTitle : category.getSeriesTitles()) {
//                List<ErrorDTO> errorDTOList = new ArrayList<>();
//                for (Error error : seriesTitle.getErrors()) {
//                    errorDTOList.add(new ErrorDTO(error.getCode(), error.getCause(), error.getDescription()));
//                }
//                seriesTitleDTO.add(new SeriesTitleDTO(seriesTitle.getTitle(),errorDTOList));
//            }
//            categoryWithTitlesList.add(new CategoryWithTitlesDTO(category.getTitle(),category.getRuTitle(), titleWithFilesList, seriesTitleDTO));
//        }
//
//        return categoryWithTitlesList;
//    }
//
//    @Setter
//    @Getter
//    // DTO class to hold category name and its titles
//    static class CategoryWithTitlesDTO {
//        private String categoryName;
//        private String ruCategoryName;
//        private List<PassportTitleWithFilesDTO> titles;
//        private List<SeriesTitleDTO> seriesTitleDTOS;
//        public CategoryWithTitlesDTO(String categoryName,String ruCategoryName, List<PassportTitleWithFilesDTO> titles, List<SeriesTitleDTO> seriesTitleDTOS) {
//            this.categoryName = categoryName;
//            this.ruCategoryName = ruCategoryName;
//            this.titles = titles;
//            this.seriesTitleDTOS = seriesTitleDTOS;
//        }
//
//        // getters and setters
//    }
//    @Setter
//    @Getter
//    // DTO class to hold title name and its files
//    static class PassportTitleWithFilesDTO {
//        private String titleName;
//        private String ruTitleName;
//        private String file;
//
//        public PassportTitleWithFilesDTO(String titleName,String ruTitleName, String file) {
//            this.titleName = titleName;
//            this.ruTitleName = ruTitleName;
//            this.file = file;
//        }
//
//        // getters and setters
//    }
//    @Setter
//    @Getter
//    static class SeriesTitleDTO {
//        private String seriesTitle;
//        private List<ErrorDTO> errors;
//
//        public SeriesTitleDTO(String seriesTitle, List<ErrorDTO> errors) {
//            this.seriesTitle = seriesTitle;
//            this.errors = errors;
//        }
//    }
//    @Setter
//    @Getter
//    static class ErrorDTO {
//        private String code;
//        private String cause;
//        private String description;
//
//        public ErrorDTO(String code, String cause, String description) {
//            this.code = code;
//            this.cause = cause;
//            this.description = description;
//        }
//    }
//}
