package fr.mossaab.security.service;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class StorageService {
    private FileDataRepository fileDataRepository;

    // Универсальный метод для загрузки изображения с передачей объекта, с которым нужно связать файл
    public Object uploadImageToFileSystem(MultipartFile file, Object relatedEntity) throws IOException {
        String name;
        FileData.FileDataBuilder builder = FileData.builder();
        name = UUID.randomUUID().toString();
        System.out.println("Received related entity type: " + relatedEntity.getClass().getSimpleName());
        System.out.println("Related entity: " + relatedEntity);
        // Устанавливаем связи в зависимости от типа объекта
        switch (relatedEntity.getClass().getSimpleName().toString()) {

            case "User":
                User user = (User) relatedEntity;
                // Удаляем старый аватар, если он существует
                if (user.getFileData() != null) {
                    fileDataRepository.delete(user.getFileData());
                }
                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/user_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/user_files/" + name + ".png"));
                }
                // Устанавливаем связь с пользователем
                builder.user(user);
                break;
            // Можно добавить дополнительные случаи для других типов объектов
            default:
                throw new IllegalArgumentException("Unsupported related entity type: " + relatedEntity.getClass().getSimpleName());
        }

        // Строим объект после завершения конфигурации
        FileData fileData = builder.build();

        // Сохраняем объект в соответствующем репозитории
        fileData = fileDataRepository.save(fileData);


        return fileData;
    }

    public Object uploadImageToFileSystemWithName(MultipartFile file, String name) throws IOException {
        FileData.FileDataBuilder builder = FileData.builder();
        builder.name(name + ".png");
        builder.type("image/png");
        builder.filePath("/var/www/vuary/explosion_diagram_files/" + name + ".png");
        if (file != null && !file.isEmpty()) {
            file.transferTo(new File("/var/www/vuary/explosion_diagram_files/" + name + ".png"));
        }


        // Строим объект после завершения конфигурации
        FileData fileData = builder.build();

        // Сохраняем объект в соответствующем репозитории
        fileData = fileDataRepository.save(fileData);


        return fileData;
    }

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<FileData> fileData = fileDataRepository.findByName(fileName);
        String filePath = fileData.get().getFilePath();
        return Files.readAllBytes(new File(filePath).toPath());
    }

}
