package fr.mossaab.security.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String nickname;
    private String email;
    private Integer pears;
}