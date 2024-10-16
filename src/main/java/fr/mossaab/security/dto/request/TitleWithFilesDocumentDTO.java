package fr.mossaab.security.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TitleWithFilesDocumentDTO {
    private String titleName;
    private String ruTitleName;
    private List<String> files;

    public TitleWithFilesDocumentDTO(String titleName,String ruTitleName, List<String> files) {
        this.titleName = titleName;
        this.ruTitleName = ruTitleName;
        this.files = files;
    }
}
