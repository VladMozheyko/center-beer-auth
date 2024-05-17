package fr.mossaab.security.entities;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "_user_id", referencedColumnName = "id", unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

}
