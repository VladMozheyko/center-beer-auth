package fr.mossaab.security.service;

import fr.mossaab.security.config.PathConfig;
import fr.mossaab.security.entities.FileData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Service
public class StorageTypeService {
    @Autowired
    private PathConfig pathConfig;
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        byte[] images = Files.readAllBytes(new File(pathConfig.getStorageTypeServiceFolderPath()+fileName).toPath());
        return images;
    }
}
