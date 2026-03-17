package fr.mossaab.security.dto.user;

import lombok.Data;

@Data
public class LocationDto {
    private Double latitude;
    private Double longitude;

    private String country;
    private String city;
}