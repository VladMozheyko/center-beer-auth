package fr.mossaab.security.service.impl;

import fr.mossaab.security.config.PathConfig;
import fr.mossaab.security.entities.BoilerPurchasePhoto;
import fr.mossaab.security.entities.BonusRequest;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.BoilerPurchasePhotoRepository;
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
public class BoilerPurchasePhotoService {

    @Autowired
    private BoilerPurchasePhotoRepository boilerPurchasePhotoRepository;

    /**
     * Загружает изображение пользователя в файловую систему.
     *
     * @param file Загружаемый файл
     * @return Данные о загруженном файле
     * @throws IOException Если происходит ошибка ввода-вывода при загрузке файла
     */
    public BoilerPurchasePhoto uploadImageToFileSystemPhotoForBonus(MultipartFile file, BonusRequest bonusRequest) throws IOException {
        String fileName = UUID.randomUUID().toString();
        String filePath = "/var/www/vuary/bonus/" + fileName + ".png";

        // Создать новый объект FileData и установить пользователя перед сохранением
        BoilerPurchasePhoto boilerPurchasePhoto = BoilerPurchasePhoto.builder()
                .name(fileName + ".png")
                .type(file.getContentType())
                .filePath(filePath)
                .bonusRequest(bonusRequest)
                .build();

        // Сохранить объект FileData в репозитории
        boilerPurchasePhoto = boilerPurchasePhotoRepository.save(boilerPurchasePhoto);

        // Перенести файл
        file.transferTo(new File(filePath));

        return  boilerPurchasePhoto;
    }


    /**
     * Загружает изображение из файловой системы.
     *
     * @param fileName Имя файла для загрузки
     * @return Байтовый массив изображения
     * @throws IOException Если происходит ошибка ввода-вывода при чтении файла
     */
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<BoilerPurchasePhoto> boilerPurchasePhoto =  boilerPurchasePhotoRepository.findByName(fileName);
        String filePath=boilerPurchasePhoto.get().getFilePath();
        return Files.readAllBytes(new File(filePath).toPath());
    }
}
