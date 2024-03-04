package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="characteristics")
@Getter
@Setter
public class Characteristic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    private Unit unit;

    @ManyToMany
    @JsonIgnore
    private List<Series> series =  new ArrayList<>();

    public Characteristic() {
        this("null", new Unit());
    }

    public Characteristic(String title, Unit unit) {
        this.id = null;
        this.title = title;
        this.unit = unit;
    }
}
