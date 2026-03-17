package fr.mossaab.security.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String nickname;
    private String email;
    private LocalDateTime createdAt;
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
}