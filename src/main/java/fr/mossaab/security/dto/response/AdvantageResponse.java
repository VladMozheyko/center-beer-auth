package fr.mossaab.security.dto.response;

import fr.mossaab.security.enums.CategoryOfAdvantage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdvantageResponse {
    private String title;
    private CategoryOfAdvantage category;
    private String iconPath;
    public AdvantageResponse(String title, CategoryOfAdvantage category, String iconPath) {
        this.title = title;
        this.category = category;
        this.iconPath = iconPath;
    }

}
