package fr.mossaab.security.entities;

import lombok.*;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="units")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Unit {

    @Id
    @GeneratedValue
    private Long id;

    private String shortName;
    private String longName;
    @ManyToMany
    @JoinTable(
            name = "unit_characteristics",
            joinColumns = @JoinColumn(name = "unit_id"),
            inverseJoinColumns = @JoinColumn(name = "characteristic_id")
    )
    private List<Characteristic> characteristics = new ArrayList<>();
}
