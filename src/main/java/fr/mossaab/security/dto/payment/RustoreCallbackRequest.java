package fr.mossaab.security.dto.payment;

import lombok.Data;

@Data
public class RustoreCallbackRequest {
    private String id;
    private String timestamp;
    private String payload; // тут будет твой Base64-шифр
}