package fr.mossaab.security.entities;

import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADVERTISEMENTS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Advertisement {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Название рекламы.
     */
    private String title;


    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Фотография, связанная с рекламой.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_id", referencedColumnName = "id", unique = true)
    private FileData fileData;

    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Integer cost;

    /**
     * Пользователь, создавший эту рекламу.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdvertisementStatus status;


    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = true)
    private AdQueueStatus queueStatus;

    @Column(name = "link")
    private String link;
}
