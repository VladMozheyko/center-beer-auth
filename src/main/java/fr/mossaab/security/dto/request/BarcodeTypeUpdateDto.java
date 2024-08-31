package fr.mossaab.security.dto.request;

import lombok.Data;

@Data
public class BarcodeTypeUpdateDto {
    private int points;
    private String type;
    private String subtype;

    // Конструктор, геттеры, сеттеры
    // ...
}