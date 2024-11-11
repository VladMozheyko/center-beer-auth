//package fr.mossaab.security.service.init;
//
//import fr.mossaab.security.entities.DocumentCategory;
//import fr.mossaab.security.entities.DocumentTitle;
//import fr.mossaab.security.repository.DocumentCategoryRepository;
//import fr.mossaab.security.repository.DocumentTitleRepository;
//import fr.mossaab.security.service.StorageService;
//import lombok.AllArgsConstructor;
//import lombok.NoArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.IOException;
//import java.util.List;
//
//@Service
//@AllArgsConstructor
//@NoArgsConstructor
//public class DocumentCreateService {
//    @Autowired
//    private StorageService storageService;
//
//    @Autowired
//    private DocumentCategoryRepository documentCategoryRepository;
//
//    @Autowired
//    private DocumentTitleRepository documentTitleRepository;
//
//
//    @Transactional
//    public void createAndSaveDocumentData() throws IOException {
//        if (documentCategoryRepository.count() == 0 && documentTitleRepository.count() == 0 ) {
//            // Создание категорий
//            DocumentCategory category1 = new DocumentCategory();
//            category1.setTitle("Processing_Policy_and_User_Agreement");
//            documentCategoryRepository.saveAll(List.of(category1));
//
//            // Создание заголовков документов
//            DocumentTitle title1 = new DocumentTitle();
//            title1.setTitle("Personal_Data_Processing_Policy");
//            title1.setRuTitle("ПОЛИТИКА ОБ ОБРАБОТКЕ ПЕРСОНАЛЬНЫХ ДАННЫХ");
//            title1.setCategory(category1);
//
//            DocumentTitle title2 = new DocumentTitle();
//            title2.setTitle("User_Agreement");
//            title2.setRuTitle("ПОЛЬЗОВАТЕЛЬСКОЕ СОГЛАШЕНИЕ");
//            title2.setCategory(category1);
//
//            documentTitleRepository.saveAll(List.of(title1, title2));
//            storageService.uploadImageToFileSystem(null,title1.getTitle(),title1);
//            storageService.uploadImageToFileSystem(null,title2.getTitle(),title2);
//        }
//    }
//
//}