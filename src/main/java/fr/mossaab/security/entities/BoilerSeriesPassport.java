package fr.mossaab.security.entities;

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
public class BoilerSeriesPassport {

    @Id
    @GeneratedValue
    private Long id;

    // Используем правильное имя, которое у вас есть в FileData
    @OneToOne(mappedBy = "boilerSeriesPassport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private FileData file;

    @ManyToMany(mappedBy = "boilerSeriesPassports")
    private List<Series> seriesList = new ArrayList<>();

}