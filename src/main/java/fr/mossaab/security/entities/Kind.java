package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="kinds")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Kind {

    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    @JsonIgnore
    private Type type;

    @OneToMany(mappedBy = "kind", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Series> series = new ArrayList<>();

}
