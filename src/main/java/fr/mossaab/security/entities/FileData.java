package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "FILE_DATA")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FileData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String filePath;
    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "_user_id",referencedColumnName = "id",unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;


}
