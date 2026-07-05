package fr.mossaab.security.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO для передачи информации о IP-адресе пользователя.
 * Содержит только публичные данные: IP и время сохранения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIpTempDto {

    private Instant createdAt;
    private String ipAddress;
}


