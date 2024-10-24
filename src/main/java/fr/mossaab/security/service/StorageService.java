package fr.mossaab.security.service;

import fr.mossaab.security.entities.*;
import fr.mossaab.security.repository.FileDataRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
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
    public Object uploadImageToFileSystem(MultipartFile file, String name, Object relatedEntity) throws IOException {
        FileData.FileDataBuilder builder = FileData.builder();
        if(Objects.equals(name, "")) {
            name = UUID.randomUUID().toString();
        }
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
                builder.filePath("/var/www/vuary/user_folder/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/user_folder/" + name + ".png"));
                }
                // Устанавливаем связь с пользователем
                builder.user(user);
                break;

            case "BonusRequest":
                BonusRequest bonusRequest = (BonusRequest) relatedEntity;

                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/bonus/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/bonus/" + name + ".png"));
                }
                // Устанавливаем связь с бонусным запросом
                builder.bonusRequest(bonusRequest);
                break;
            case "DocumentVerificationRequest":
                DocumentVerificationRequest documentVerification = (DocumentVerificationRequest) relatedEntity;

                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/documentVerification/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/documentVerification/" + name + ".png"));
                }
                // Устанавливаем связь с бонусным запросом
                builder.documentVerification(documentVerification);
                break;
            case "PassportTitle":
                builder.name(name + ".pdf");
                builder.type("application/pdf");
                builder.filePath("/var/www/vuary/Guidance_and_error_codes/" + name + ".pdf");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/Guidance_and_error_codes/" + name + ".pdf"));
                }
                if (relatedEntity instanceof PassportTitle) {
                    PassportTitle passportTitle = (PassportTitle) relatedEntity;
                    builder.passportTitle(passportTitle);
                }
                break;  // Добавляем break для завершения этого case

            case "DocumentTitle":
                builder.name(name + ".pdf");
                builder.type("application/pdf");
                builder.filePath("/var/www/vuary/Processing_Policy_and_User_Agreement/" + name + ".pdf");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/Processing_Policy_and_User_Agreement/" + name + ".pdf"));
                }
                DocumentTitle documentTitle = (DocumentTitle) relatedEntity;
                builder.documentTitle(documentTitle);
                break;  // Добавляем break для завершения этого case
            case "Series":
                Series series = (Series) relatedEntity;
                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/Series/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/Series/" + name + ".png"));
                }
                builder.series(series);
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

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<FileData> fileData = fileDataRepository.findByName(fileName);
        String filePath = fileData.get().getFilePath();
        return Files.readAllBytes(new File(filePath).toPath());
    }

}
