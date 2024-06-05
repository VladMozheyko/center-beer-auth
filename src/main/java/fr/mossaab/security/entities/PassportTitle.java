package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "passport_title")
@Getter
@Setter
public class PassportTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;
    private String ruTitle;

    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID")
    @JsonBackReference
    private PassportCategory category;

    @OneToMany(mappedBy = "passportTitle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PassportFileData> files;
}