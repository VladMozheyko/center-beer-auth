package fr.mossaab.security.dto.request;

import lombok.Data;

@Data
public class BarcodeTypeDto {
    private Long id;
    private int points;
    private String type;
    private String subtype;

    public BarcodeTypeDto(Long id, int points, String type, String subtype) {
        this.id = id;
        this.points = points;
        this.type = type;
        this.subtype = subtype;
    }
}