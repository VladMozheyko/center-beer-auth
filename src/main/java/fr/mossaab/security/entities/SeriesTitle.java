package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "series_title")
@Getter
@Setter
public class SeriesTitle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;

    @OneToMany(mappedBy = "seriesTitle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Error> errors;

    @ManyToOne
    @JoinColumn(name = "passport_category_id")
    @JsonBackReference
    private PassportCategory passportCategory;
}
