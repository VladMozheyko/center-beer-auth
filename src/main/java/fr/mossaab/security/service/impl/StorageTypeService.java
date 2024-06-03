package fr.mossaab.security.service.impl;

import fr.mossaab.security.config.PathConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class StorageTypeService {
    @Autowired
    private PathConfig pathConfig;
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        byte[] images = Files.readAllBytes(new File(pathConfig.getStorageTypeServiceFolderPath()+fileName).toPath());
        return images;
    }
}
