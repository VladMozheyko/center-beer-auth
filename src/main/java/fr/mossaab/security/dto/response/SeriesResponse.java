package fr.mossaab.security.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public  class SeriesResponse {
    private String title;
    private String description;
    private List<AdvantageResponse> advantages;

    public SeriesResponse(String title, String description, List<AdvantageResponse> advantages) {
        this.title = title;
        this.description = description;
        this.advantages = advantages;
    }


}
