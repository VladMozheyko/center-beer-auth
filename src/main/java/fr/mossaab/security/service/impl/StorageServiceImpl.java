package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.FileData;
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
public class StorageServiceImpl {


    @Autowired
    private FileDataRepository fileDataRepository;

    private final String FOLDER_PATH="C:/Users/Admin/Desktop/Vuary_test1602/src/main/resources/";

    public FileData uploadImageToFileSystemAvatarUser(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString();

        String filePath = FOLDER_PATH + fileName + ".png";

        FileData fileData=fileDataRepository.save(FileData.builder()
                .name(fileName + ".png")
                .type(file.getContentType())
                .filePath(filePath).build());
        fileDataRepository.save(fileData);
        file.transferTo(new File(filePath));

        if (fileData != null) {
            return fileData;
        }
        return null;
    }
    public FileData uploadImageToFileSystemDefaultAvatar() throws IOException {
        String fileName = "defaultAvatar.png";

        String filePath = FOLDER_PATH + fileName + ".png";

        FileData fileData=fileDataRepository.save(FileData.builder()
                .name("defaultAvatar.png")
                .type("image/jpeg")
                .filePath(filePath).build());

        //file.transferTo(new File(filePath));

        if (fileData != null) {
            return fileData;
        }
        return null;
    }

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<FileData> fileData = fileDataRepository.findByName(fileName);
        String filePath=fileData.get().getFilePath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }



}
