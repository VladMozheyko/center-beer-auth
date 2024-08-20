package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Сущность для хранения обновляемых токенов.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class RefreshToken {

    /**
     * Уникальный идентификатор токена.
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Пользователь, связанный с токеном.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    /**
     * Токен обновления.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Дата истечения токена.
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Флаг отозванности токена.
     */
    public boolean revoked;

}
