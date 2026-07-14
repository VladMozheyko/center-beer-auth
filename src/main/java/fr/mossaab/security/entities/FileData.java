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
@Table(name = "FILE_DATA", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "type"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
    private User user;

}
