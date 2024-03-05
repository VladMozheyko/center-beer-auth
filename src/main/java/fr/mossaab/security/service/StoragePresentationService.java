package fr.mossaab.security.service;

import fr.mossaab.security.entities.FileDataPresentation;
import fr.mossaab.security.entities.Presentation;
import fr.mossaab.security.repository.FileDataPresentationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class StoragePresentationService {
    //Раскомментировать и изменить под свою локальную структуру

    /*private final String FOLDER_PATH = "C:/Users/Admin/Desktop/pre/";*/
    //Закомментировать в случае локального использования
    private final String FOLDER_PATH="/var/www/vuary/";
    @Autowired
    private FileDataPresentationRepository fileDataPresentationRepository;

    public FileDataPresentation uploadImageToSql(Presentation presentation, String number) throws IOException {
        String str = presentation.getTitle();
        str = str.substring(0, str.length() - 1);

        String filePath = FOLDER_PATH + str + "/" + presentation.getTitle() + number + ".png";

        FileDataPresentation fileDataPresentation = fileDataPresentationRepository.save(FileDataPresentation.builder()
                .name(presentation.getTitle() + number + ".png")
                .type("image/jpeg")
                .filePath(filePath).build());
        fileDataPresentationRepository.save(fileDataPresentation);
        return fileDataPresentation;
    }

    public List<byte[]> downloadImagesPresentationFromFileSystem(String prefix) throws IOException {
        List<FileDataPresentation> allFileDataPresentations = fileDataPresentationRepository.findAll();
        List<byte[]> imagesList = new ArrayList<>();
        for (FileDataPresentation fileDataPresentation : allFileDataPresentations) {
            String fileName = fileDataPresentation.getName();
            if (fileName.startsWith(prefix)) {
                String filePath = fileDataPresentation.getFilePath();
                byte[] imageBytes = Files.readAllBytes(new File(filePath).toPath());
                imagesList.add(imageBytes);
            }
        }
        return imagesList;
    }
}
