package fr.mossaab.security.service;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.FileDataPresentation;
import fr.mossaab.security.entities.Presentation;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataPresentationRepository;
import fr.mossaab.security.repository.FileDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
@Service
public class StoragePresentationService {

    @Autowired
    private FileDataPresentationRepository fileDataPresentationRepository;

    private final String FOLDER_PATH="/var/www/vuary/";

    public FileDataPresentation uploadImageToSql(Presentation presentation,String number) throws IOException {

        String filePath = FOLDER_PATH + presentation.getTitle() + number + ".png";

        FileDataPresentation fileDataPresentation=fileDataPresentationRepository.save(FileDataPresentation.builder()
                .name(presentation.getTitle()+number+".png")
                .type("image/jpeg")
                .filePath(filePath).build());
        fileDataPresentationRepository.save(fileDataPresentation);
        return fileDataPresentation;
    }

    public byte[] downloadImagePresentationFromFileSystem(String fileName) throws IOException {
        Optional<FileDataPresentation> fileDataPresentation = fileDataPresentationRepository.findByName(fileName);
        String filePath=fileDataPresentation.get().getFilePath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }
}
