package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="boilers")
@Getter
@Setter
public class Boiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    @JsonIgnore
    private Series series;

    @ManyToMany(mappedBy = "boilers")
    private List<Value> values =  new ArrayList<>();

    public Boiler() {
        this("null", new Series());
    }

    public Boiler(String title, Series series) {
        this.id = null;
        this.title = title;
        this.series = series;
    }
}
