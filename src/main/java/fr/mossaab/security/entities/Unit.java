package fr.mossaab.security.entities;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name="units")
@Setter
@Getter
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shortName;
    private String longName;

    public Unit() {
        this("null", "null");
    }

    public Unit(String shortName, String longName) {
        this.id = null;
        this.shortName = shortName;
        this.longName = longName;
    }
}
