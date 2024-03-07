package fr.mossaab.security.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class StorageAdvantageService {
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        byte[] images = Files.readAllBytes(new File("/var/www/vuary/IconsAdvantage/"+fileName).toPath());
        return images;
    }
}
