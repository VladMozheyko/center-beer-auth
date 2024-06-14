package fr.mossaab.security.controller;

import fr.mossaab.security.entities.DocumentCategory;
import fr.mossaab.security.entities.DocumentFileData;
import fr.mossaab.security.entities.DocumentTitle;
import fr.mossaab.security.repository.DocumentCategoryRepository;
import fr.mossaab.security.repository.DocumentFileDataRepository;
import fr.mossaab.security.repository.DocumentTitleRepository;
import fr.mossaab.security.service.impl.DocumentService;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    @Autowired
    private DocumentService documentService;
    @Autowired
    private DocumentTitleRepository documentTitleRepository;
    @Autowired
    private DocumentFileDataRepository documentFileDataRepository;
    @Autowired
    private DocumentCategoryRepository documentCategoryRepository;

    @GetMapping("/categories")
    public List<CategoryWithTitlesDTO> getAllCategoriesWithTitlesAndFiles() {
        List<CategoryWithTitlesDTO> categoryWithTitlesList = new ArrayList<>();

        List<DocumentCategory> categories = documentCategoryRepository.findAll();
        for (DocumentCategory category : categories) {
            List<TitleWithFilesDTO> titleWithFilesList = new ArrayList<>();
            List<DocumentTitle> titles = documentTitleRepository.findAllByCategory(category);
            for (DocumentTitle title : titles) {
                List<DocumentFileData> fileDataList = documentFileDataRepository.findByDocumentTitle(title);
                List<String> filePaths = fileDataList.stream()
                        .map(fileData -> "http://31.129.102.70:8080/documents/fileSystem/" + fileData.getName()) // Добавление хоста к каждому имени файла
                        .collect(Collectors.toList()); // Собираем все имена файлов в список

                titleWithFilesList.add(new TitleWithFilesDTO(title.getTitle(),title.getRuTitle(), filePaths));
            }
            categoryWithTitlesList.add(new CategoryWithTitlesDTO(category.getTitle(), titleWithFilesList));
        }

        return categoryWithTitlesList;
    }

    /*@GetMapping("/file/{fileName}")
    public byte[] getDocument(@PathVariable String fileName) throws IOException {
        return documentService.downloadDocumentFromFileSystem(fileName);
    }*/

    @GetMapping("/fileSystem/{fileName}")
    public ResponseEntity<?> downloadImageFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = documentService.downloadDocumentFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }


    @Setter
    @Getter
    static class CategoryWithTitlesDTO {
        private String categoryName;
        private List<TitleWithFilesDTO> titles;

        public CategoryWithTitlesDTO(String categoryName, List<TitleWithFilesDTO> titles) {
            this.categoryName = categoryName;
            this.titles = titles;
        }
    }

    @Setter
    @Getter
    static class TitleWithFilesDTO {
        private String titleName;
        private String ruTitleName;
        private List<String> files;

        public TitleWithFilesDTO(String titleName,String ruTitleName, List<String> files) {
            this.titleName = titleName;
            this.ruTitleName = ruTitleName;
            this.files = files;
        }
    }
}


