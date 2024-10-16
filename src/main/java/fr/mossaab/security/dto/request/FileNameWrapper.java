package fr.mossaab.security.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileNameWrapper {
    private String fileName;

    // Геттер и сеттер для имени файла
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
