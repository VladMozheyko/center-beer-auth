package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.mossaab.security.enums.CategoryOfAdvantage;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "advantages")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Advantage {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private CategoryOfAdvantage category;

    @ManyToMany
    @JsonIgnore
    private List<Series> series = new ArrayList<>();

    /**
     * Связь с FileData один-к-одному.
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "file_data_id", referencedColumnName = "id")
    private FileData fileData;


}
