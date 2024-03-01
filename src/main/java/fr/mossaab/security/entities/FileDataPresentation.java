package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "FILE_DATA_PRESENTATION")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FileDataPresentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String filePath;
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "_presentation_id",referencedColumnName = "id",unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Presentation presentation;


}
