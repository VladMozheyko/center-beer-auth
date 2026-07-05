package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Сущность для временного хранения IP-адресов пользователей.
 * IP-адреса хранятся с TTL и пометкой, являются ли они приватными.
 */
@Entity
@Table(
        name = "user_ip_temp",
        indexes = {
                @Index(name = "idx_user_ip_temp_user_id", columnList = "userId"),
                @Index(name = "idx_user_ip_temp_expires_at", columnList = "expiresAt")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIpTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(nullable = false)
    @JsonIgnore
    private Long userId;

    @Column(length = 45, nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    @JsonIgnore
    private Boolean isPrivateOrLoopback = false;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @JsonIgnore
    private Instant expiresAt;
}