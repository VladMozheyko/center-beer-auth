package fr.mossaab.security.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * DTO для передачи информации о IP-адресе пользователя.
 * Содержит только публичные данные: IP и время сохранения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIpTempDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;
    private String ipAddress;

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = LocalDateTime.ofInstant(createdAt, ZoneId.of("Europe/Moscow"));
    }
}


