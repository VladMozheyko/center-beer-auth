package fr.mossaab.security.dto.user;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPointsResponse {
    private int position;
    private String nickname;
    private int points;
}