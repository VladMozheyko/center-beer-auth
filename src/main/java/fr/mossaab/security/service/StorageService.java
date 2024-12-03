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
    public Object uploadImageToFileSystem(MultipartFile file, Object relatedEntity) throws IOException {
        String name;
        FileData.FileDataBuilder builder = FileData.builder();
        name = UUID.randomUUID().toString();
        System.out.println("Received related entity type: " + relatedEntity.getClass().getSimpleName());
        System.out.println("Related entity: " + relatedEntity);
        // Устанавливаем связи в зависимости от типа объекта
        switch (relatedEntity.getClass().getSimpleName().toString()) {

            case "ExplosionDiagram":
                ExplosionDiagram explosionDiagram = (ExplosionDiagram) relatedEntity;
                // Удаляем старый аватар, если он существует
                if (explosionDiagram.getFileData() != null) {
                    fileDataRepository.delete(explosionDiagram.getFileData());
                }
                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/explosion_diagram_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/explosion_diagram_files/" + name + ".png"));
                }
                // Устанавливаем связь с пользователем
                builder.explosionDiagram(explosionDiagram);
                break;
            case "Advantage":
                Advantage advantage = (Advantage) relatedEntity;
                // Удаляем старый аватар, если он существует
                if (advantage.getFileData() != null) {
                    fileDataRepository.delete(advantage.getFileData());
                }
                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/advantage_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/advantage_files/" + name + ".png"));
                }
                // Устанавливаем связь с пользователем
                builder.advantage(advantage);
                break;
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

            case "BonusRequest":
                BonusRequest bonusRequest = (BonusRequest) relatedEntity;

                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/bonus_request_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/bonus_request_files/" + name + ".png"));
                }
                // Устанавливаем связь с бонусным запросом
                builder.bonusRequest(bonusRequest);
                break;
            case "DocumentVerificationRequest":
                DocumentVerificationRequest documentVerification = (DocumentVerificationRequest) relatedEntity;

                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/document_verification_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/document_verification_files/" + name + ".png"));
                }
                // Устанавливаем связь с бонусным запросом
                builder.documentVerification(documentVerification);
                break;
            case "SparePart":
                SparePart sparePart = (SparePart) relatedEntity;

                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/spare_part_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/spare_part_files/" + name + ".png"));
                }
                // Устанавливаем связь с бонусным запросом
                builder.sparePart(sparePart);
                break;
            case "BoilerSeriesPassport":
                builder.name(name + ".pdf");
                builder.type("application/pdf");
                builder.filePath("/var/www/vuary/boiler_series_passport_files/" + name + ".pdf");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/boiler_series_passport_files/" + name + ".pdf"));
                }
                if (relatedEntity instanceof BoilerSeriesPassport) {
                    BoilerSeriesPassport boilerSeriesPassport = (BoilerSeriesPassport) relatedEntity;
                    builder.boilerSeriesPassport(boilerSeriesPassport);
                }
                break;  // Добавляем break для завершения этого case

//            case "DocumentTitle":
//                builder.name(name + ".pdf");
//                builder.type("application/pdf");
//                builder.filePath("/var/www/vuary/Processing_Policy_and_User_Agreement/" + name + ".pdf");
//                if (file != null && !file.isEmpty()) {
//                    file.transferTo(new File("/var/www/vuary/Processing_Policy_and_User_Agreement/" + name + ".pdf"));
//                }
//                DocumentTitle documentTitle = (DocumentTitle) relatedEntity;
//                builder.documentTitle(documentTitle);
//                break;  // Добавляем break для завершения этого case
            case "Series":
                Series series = (Series) relatedEntity;
                builder.name(name + ".png");
                builder.type("image/png");
                builder.filePath("/var/www/vuary/series_files/" + name + ".png");
                if (file != null && !file.isEmpty()) {
                    file.transferTo(new File("/var/www/vuary/series_files/" + name + ".png"));
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
