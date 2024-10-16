package fr.mossaab.security.controller;

import fr.mossaab.security.dto.request.CategoryWithTitlesDTO;
import fr.mossaab.security.dto.request.ErrorDTO;
import fr.mossaab.security.dto.request.PassportTitleWithFilesDTO;
import fr.mossaab.security.dto.request.SeriesTitleDTO;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.entities.Error;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.PassportCategoryRepository;
import fr.mossaab.security.repository.PassportTitleRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/passport")
@RequiredArgsConstructor
public class PassportController {

//    private final PassportCategoryRepository passportCategoryRepository;
//    private final PassportTitleRepository passportTitleRepository;
//    private final FileDataRepository passportFileDataRepository;
//    private final PassportService passportService;
//    @GetMapping("/categories")
//    public List<CategoryWithTitlesDTO> getAllCategoriesWithTitlesAndFiles() {
//        List<CategoryWithTitlesDTO> categoryWithTitlesList = new ArrayList<>();
//
//        List<PassportCategory> categories = passportCategoryRepository.findAll();
//        for (PassportCategory category : categories) {
//            List<PassportTitleWithFilesDTO> titleWithFilesList = new ArrayList<>();
//            List<PassportTitle> titles = passportTitleRepository.findAllByCategory(category);
//            for (PassportTitle title : titles) {
//                List<FileData> fileDataList = passportFileDataRepository.findByName(title);
//                // Используем цикл для добавления хоста к каждому имени файла
//                List<String> filePaths = new ArrayList<>();
//                for (FileData fileData : fileDataList) {
//                    String filePath = "http://31.129.102.70:8080/passport/image/" + fileData.getName();
//                    filePaths.add(filePath);
//                }
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



}
