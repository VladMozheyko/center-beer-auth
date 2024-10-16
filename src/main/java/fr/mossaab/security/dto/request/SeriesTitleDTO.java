package fr.mossaab.security.dto.request;

import fr.mossaab.security.controller.PassportController;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SeriesTitleDTO {
    private String seriesTitle;
    private List<ErrorDTO> errors;

    public SeriesTitleDTO(String seriesTitle, List<ErrorDTO> errors) {
        this.seriesTitle = seriesTitle;
        this.errors = errors;
    }
}
