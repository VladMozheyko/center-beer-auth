package fr.mossaab.security.dto.advertisement;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdvertisementResponse {
    private Long id;
    private int position;
    private int cost;
    private String nickname;
    private Long fileDataId;
    private String link;
}
