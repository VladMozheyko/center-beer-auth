package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "passport_title")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PassportTitle {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "passportTitle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private FileData file;

    @ManyToMany(mappedBy = "passportTitles")
    private List<Series> seriesList = new ArrayList<>();

}