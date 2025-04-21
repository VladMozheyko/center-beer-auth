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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "advertisement")
public class FileData {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    private String name;
    private String type;
    private String filePath;

    @OneToOne(optional = true)
    @JoinColumn(name = "_user_id", referencedColumnName = "id", unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    @EqualsAndHashCode.Exclude
    private User user;

    @OneToOne(mappedBy = "fileData", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Advertisement advertisement;
}
