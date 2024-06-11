package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "error")
@Getter
@Setter
public class Error {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String code;
    private String cause;
    private String description;

    @ManyToOne
    @JoinColumn(name = "series_title_id")
    @JsonBackReference
    private SeriesTitle seriesTitle;
}
