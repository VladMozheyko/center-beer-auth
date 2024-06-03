package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passport_file_data")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PassportFileData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "PASSPORT_TITLE_ID")
    @JsonBackReference
    private PassportTitle passportTitle;
}
 /*@ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "_presentation_id",referencedColumnName = "id",unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Presentation presentation;*/
