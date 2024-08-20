package fr.mossaab.security.dto.request;

import lombok.Data;


@Data
public class FileDataDTO {
    private Long id;
    private String name;
    private String type;
    private String filePath;
}