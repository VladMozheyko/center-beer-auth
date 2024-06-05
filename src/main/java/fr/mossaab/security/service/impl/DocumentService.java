package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.DocumentCategory;
import fr.mossaab.security.entities.DocumentFileData;
import fr.mossaab.security.entities.DocumentTitle;
import fr.mossaab.security.repository.DocumentCategoryRepository;
import fr.mossaab.security.repository.DocumentFileDataRepository;
import fr.mossaab.security.repository.DocumentTitleRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class DocumentService {
    @Autowired
    private DocumentFileDataRepository documentFileDataRepository;

    @Autowired
    private DocumentCategoryRepository documentCategoryRepository;

    @Autowired
    private DocumentTitleRepository documentTitleRepository;

    public DocumentFileData uploadDocument(DocumentTitle documentTitle, String number) throws IOException{
        String str1 = "/var/www/vuary/Политика обработки и соглашения";
        String str2 = documentTitle.getCategory().getTitle();
        String str3 = documentTitle.getTitle();
        String filePath = str1 + str2 + "/" + str3 + "/" + number + ".png";

        DocumentFileData documentFileData = (DocumentFileData) documentFileDataRepository.save(DocumentFileData.builder()
                .name(str3 + number + ".png")
                .type("image/jpeg")
                .filePath(filePath)
                .documentTitle(documentTitle)
                .build());
        return documentFileData;
    }

    public byte[] downloadDocumentFromFileSystem(String fileName) throws IOException {
        Optional<DocumentFileData> fileData = documentFileDataRepository.findByName(fileName);
        if (fileData.isPresent()) {
            String filePath = fileData.get().getFilePath();
            return Files.readAllBytes(new File(filePath).toPath());
        }
        throw new IOException("File not found");
    }

    @Transactional
    public void createAndSaveDocumentData() throws IOException {
        if (documentCategoryRepository.count() == 0 && documentTitleRepository.count() == 0 && documentFileDataRepository.count() == 0) {
            // Создание категорий
            DocumentCategory category1 = new DocumentCategory();
            category1.setTitle("Политика обработки персональных данных и соглашения");
            documentCategoryRepository.saveAll(List.of(category1));

            // Создание заголовков документов
            DocumentTitle title1 = new DocumentTitle();
            title1.setTitle("Политика об обработке персональных данных");
            title1.setCategory(category1);

            DocumentTitle title2 = new DocumentTitle();
            title2.setTitle("Пользовательское соглашение");
            title2.setCategory(category1);

            documentTitleRepository.saveAll(List.of(title1, title2));

            // Загрузка документов
            for (int i = 1; i <= 12; i++) {
                uploadDocument(title1, "-" + i);
            }
            for (int i = 1; i <= 12; i++) {
                uploadDocument(title2, "-" + i);
            }
        }
    }

}
