package fr.mossaab.security.backup.project.dto.v1;


import lombok.Data;

@Data
public class LocationBackupDtoV1 {

    private Long id; // ID из исходной БД

    private Double latitude;
    private Double longitude;
    private String country;
    private String city;

//    private Long userOriginalId;  // храним только у владельца связи
}
