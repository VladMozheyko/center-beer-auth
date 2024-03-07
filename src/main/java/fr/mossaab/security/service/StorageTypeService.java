package fr.mossaab.security.service;

import fr.mossaab.security.entities.FileData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class StorageTypeService {
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        byte[] images = Files.readAllBytes(new File("/var/www/vuary/types/"+fileName).toPath());
        return images;
    }
}
