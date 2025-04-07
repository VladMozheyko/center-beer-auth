package fr.mossaab.security.service;

import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для работы с хранилищем файлов.
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final FileDataRepository fileDataRepository;

    @Value("${app.upload-path}")
    private String uploadBasePath;

    private final String[] requiredFolders = {
            "user_files",
            "advertisement_files"
    };

    @PostConstruct
    public void initDirectories() {
        for (String folder : requiredFolders) {
            File dir = new File(uploadBasePath + "/" + folder);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    System.out.println("📂 Создана директория: " + dir.getAbsolutePath());
                } else {
                    System.err.println("⚠️ Не удалось создать директорию: " + dir.getAbsolutePath());
                }
            } else {
                System.out.println("✅ Директория уже существует: " + dir.getAbsolutePath());
            }
        }
    }

    public Object uploadImageToFileSystem(MultipartFile file, Object relatedEntity) throws IOException {
        String name = UUID.randomUUID().toString();
        FileData.FileDataBuilder builder = FileData.builder();

        System.out.println("📥 Received related entity type: " + relatedEntity.getClass().getSimpleName());

        switch (relatedEntity.getClass().getSimpleName()) {
            case "User" -> {
                User user = (User) relatedEntity;
                if (user.getFileData() != null) {
                    fileDataRepository.delete(user.getFileData());
                }
                String path = uploadBasePath + "/user_files/" + name + ".png";
                saveFile(file, path);
                builder.name(name + ".png").type("image/png").filePath(path).user(user);
            }
            case "Advertisement" -> {
                Advertisement advertisement = (Advertisement) relatedEntity;
                String path = uploadBasePath + "/advertisement_files/" + name + ".png";
                saveFile(file, path);
                builder.name(name + ".png").type("image/png").filePath(path).advertisement(advertisement);
            }
            default -> throw new IllegalArgumentException("Unsupported related entity type: " + relatedEntity.getClass().getSimpleName());
        }

        return fileDataRepository.save(builder.build());
    }

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<FileData> fileData = fileDataRepository.findByName(fileName);
        String filePath = fileData.orElseThrow(() -> new RuntimeException("Файл не найден")).getFilePath();
        return Files.readAllBytes(new File(filePath).toPath());
    }

    private void saveFile(MultipartFile file, String fullPath) throws IOException {
        if (file != null && !file.isEmpty()) {
            File targetFile = new File(fullPath);
            file.transferTo(targetFile);
        }
    }
}
