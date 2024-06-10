package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Сущность отображающая категории паспортов: Газовые котлы, электрические колонки и т.д.
 */
@Entity
@Table(name = "passport_category")
@Getter
@Setter
public class PassportCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;

    @OneToMany(mappedBy = "passportCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SeriesTitle> seriesTitles;
}