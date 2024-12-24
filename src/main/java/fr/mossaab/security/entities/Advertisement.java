package fr.mossaab.security.entities;

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

    /**
     * Описание рекламы.
     */
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
}
