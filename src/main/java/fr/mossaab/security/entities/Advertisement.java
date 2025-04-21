package fr.mossaab.security.entities;

import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADVERTISEMENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "fileData")
public class Advertisement {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_id", referencedColumnName = "id", unique = true)
    @EqualsAndHashCode.Exclude
    private FileData fileData;

    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Integer cost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdvertisementStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status")
    private AdQueueStatus queueStatus;

    private String link;
}
