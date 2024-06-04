package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    private String series;

    private String description;
    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID")
    @JsonBackReference
    private PassportCategory category;

}

