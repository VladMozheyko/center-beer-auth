package fr.mossaab.security.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class LocationDto {
    private Double latitude;
    private Double longitude;

    private String country;
    private String city;
}