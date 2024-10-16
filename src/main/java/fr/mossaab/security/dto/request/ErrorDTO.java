package fr.mossaab.security.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorDTO {
    private String code;
    private String cause;
    private String description;

    public ErrorDTO(String code, String cause, String description) {
        this.code = code;
        this.cause = cause;
        this.description = description;
    }
}
