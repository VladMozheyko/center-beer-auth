package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.ImageForSeries;
import fr.mossaab.security.repository.ImageForSeriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Service
public class StorageSeriesService {
    @Autowired
    private ImageForSeriesRepository imageForSeriesRepository;
    //Раскомментировать и изменить под свою локальную структуру
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<ImageForSeries> fileData = imageForSeriesRepository.findByName(fileName);
        String filePath=fileData.get().getFilePath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }
}
