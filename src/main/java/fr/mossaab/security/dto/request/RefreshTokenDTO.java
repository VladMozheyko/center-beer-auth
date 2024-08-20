package fr.mossaab.security.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class RefreshTokenDTO {
    private long id;
    private String token;
    private Instant expiryDate;
    private boolean revoked;
}