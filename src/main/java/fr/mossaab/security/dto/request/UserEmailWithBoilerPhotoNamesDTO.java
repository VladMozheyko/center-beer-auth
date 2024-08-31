package fr.mossaab.security.dto.request;

import lombok.Data;

import java.util.List;
@Data
public class UserEmailWithBoilerPhotoNamesDTO {
    private String email;
    private List<String> boilerPurchasePhotoNames;
    public UserEmailWithBoilerPhotoNamesDTO(String email, List<String> photoNames) {
        this.email = email;
        this.boilerPurchasePhotoNames= photoNames;
    }
}
