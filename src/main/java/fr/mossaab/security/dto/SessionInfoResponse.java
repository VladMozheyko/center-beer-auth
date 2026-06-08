package fr.mossaab.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoResponse {
    private Long id;
    private String token;
    private Instant expiryDate;
    private boolean revoked;
    private Instant createdAt;
    private Instant lastUsedAt;
    private String deviceInfo;
}
