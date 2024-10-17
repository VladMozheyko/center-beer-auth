package fr.mossaab.security.service;

import fr.mossaab.security.entities.*;
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
    public Object uploadImageToFileSystem(MultipartFile file,String name, Object relatedEntity) throws IOException {
        FileData.FileDataBuilder builder = FileData.builder();
        // Устанавливаем связи в зависимости от типа объекта
        switch (relatedEntity.getClass().getSimpleName()) {
            case "User":
                User user = (User) relatedEntity;
                // Удаляем старый аватар, если он существует
                if (user.getFileData() != null) {
                    fileDataRepository.delete(user.getFileData());
                }
                builder.name(UUID.randomUUID().toString() + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/user_folder/" + UUID.randomUUID().toString() + ".png");
                if (!file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/user_folder/" + UUID.randomUUID().toString() + ".png"));
                }
                // Устанавливаем связь с пользователем
                builder.user(user);
                break;

            case "BonusRequest":
                BonusRequest bonusRequest = (BonusRequest) relatedEntity;

                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/bonus/" + name + ".png");
                if (!file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/bonus/" + name + ".png"));
                }
                // Устанавливаем связь с бонусным запросом
                builder.bonusRequest(bonusRequest);
                break;
            case "PassportTitle":
                builder.name(name + ".png");
                builder.type("application/pdf");
                builder.filePath("/var/www/vuary/Guidance_and_error_codes/"+ name + ".pdf");
                if (!file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/Guidance_and_error_codes/"+ name + ".pdf"));
                }
                PassportTitle passportTitle = (PassportTitle) relatedEntity;
                builder.passportTitle(passportTitle);
            case "DocumentTitle":
                builder.name(name + ".png");
                builder.type("application/pdf");
                builder.filePath("/var/www/vuary/Processing_Policy_and_User_Agreement/"+ name + ".pdf");
                if (!file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/Processing_Policy_and_User_Agreement/" + name + ".pdf"));
                }
                assert relatedEntity instanceof DocumentTitle;
                DocumentTitle documentTitle = (DocumentTitle) relatedEntity;
                builder.documentTitle(documentTitle);
            case "Series":
                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/Series/" + name + ".png");
                if (!file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/Series/" + name + ".png"));
                }
                assert relatedEntity instanceof Series;
                Series series = (Series) relatedEntity;
                builder.series(series);
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
