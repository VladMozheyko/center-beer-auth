package fr.mossaab.security.backup.project.dto.v1;


import lombok.Data;

@Data
public class FileDataBackupDtoV1 {

    private Long id;    // ID из исходной БД
    private String name;
    private String type;
    private String filePath;

    private Long userOriginalId;   // если есть связь обратно на user
}
