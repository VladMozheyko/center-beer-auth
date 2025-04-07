package fr.mossaab.security.dto.advertisement;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdTimeLeftResponse {
    private int minutes;
    private int seconds;
    private String text;
}