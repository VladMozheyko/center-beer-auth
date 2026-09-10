package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Сущность для хранения последних IP-адресов пользователей.
 * Хранится не более 5 последних IP-адресов.
 */
@Entity
@Table(
        name = "user_ip_temp",
        indexes = {
                @Index(name = "idx_user_ip_temp_user_id", columnList = "userId")
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
    @Builder.Default
    private Boolean isPrivateOrLoopback = false;

    @Column(nullable = false)
    private Instant createdAt;
}