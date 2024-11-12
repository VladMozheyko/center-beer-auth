package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Сущность для хранения данных файла.
 */
@Entity
@Table(name = "FILE_DATA")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FileData {

    /**
     * Уникальный идентификатор файла.
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Имя файла.
     */
    private String name;

    /**
     * Тип файла.
     */
    private String type;

    /**
     * Путь к файлу.
     */
    private String filePath;

    /**
     * Пользователь, связанный с файлом.
     */
    @OneToOne(optional = true)
    @JoinColumn(name = "_user_id", referencedColumnName = "id", unique = true, nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private User user;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "bonus_request_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private BonusRequest bonusRequest;

    @OneToOne
    @JoinColumn(name = "passport_title_id", unique = true, nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private BoilerSeriesPassport boilerSeriesPassport;

    /**
     * Родительская сущность Series, связанная с файлом.
     */
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private Series series;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_verification_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private DocumentVerificationRequest documentVerification;
    /**
     * Связь с запчастью (SparePart), к которой привязан файл.
     */
    @OneToOne(mappedBy = "fileData", optional = true)
    @JsonBackReference
    private SparePart sparePart;

    @OneToOne(mappedBy = "fileData", optional = true)
    @JsonBackReference
    private Advantage advantage;

    @OneToOne(mappedBy = "fileData", optional = true)
    @JsonBackReference
    private ExplosionDiagram explosionDiagram;
}
