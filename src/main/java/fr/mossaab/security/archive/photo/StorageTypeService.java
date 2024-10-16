package fr.mossaab.security.archive.photo;

import fr.mossaab.security.config.PathConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
/*
@Service
@AllArgsConstructor
public class StorageTypeService {
    private PathConfig pathConfig;
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        byte[] images = Files.readAllBytes(new File(pathConfig.getStorageTypeServiceFolderPath()+fileName).toPath());
        return images;
    }
}

 */
