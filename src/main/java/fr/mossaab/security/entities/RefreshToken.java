package fr.mossaab.security.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Schema(description = "Сущность для хранения обновляемых токенов")
public class RefreshToken {

    @Id
    @GeneratedValue
    @Schema(description = "Уникальный идентификатор токена")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @Schema(description = "Пользователь, связанный с токеном")
    private User user;

    @Column(nullable = false, unique = true)
    @Schema(description = "Токен обновления")
    private String token;

    @Column(nullable = false)
    @Schema(description = "Дата истечения токена")
    private Instant expiryDate;

    @Schema(description = "Флаг отозванности токена")
    public boolean revoked;

    @Schema(description="Дата создания")
    private Instant createdAt;

    @Schema(description="Дата последнего использования")
    private Instant lastUsedAt;

    @Schema(description = "Информация об устройстве/клиенте")
    private String deviceInfo;

    @Schema(description = "Идентификатор устройства/клиента")
    @Column(length = 100)
    private String deviceId;

}
