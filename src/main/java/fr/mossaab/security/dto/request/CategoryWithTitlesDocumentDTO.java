package fr.mossaab.security.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CategoryWithTitlesDocumentDTO {
    private String categoryName;
    private List<TitleWithFilesDocumentDTO> titles;

    public CategoryWithTitlesDocumentDTO(String categoryName, List<TitleWithFilesDocumentDTO> titles) {
        this.categoryName = categoryName;
        this.titles = titles;
    }
}
