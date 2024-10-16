package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;

    @ManyToOne
    @JsonIgnore
    private Kind kind;

    @ManyToMany(mappedBy = "series")
    private List<Characteristic> characteristics = new ArrayList<>();

    @ManyToMany(mappedBy = "series")
    private List<Advantage> advantages = new ArrayList<>();

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Boiler> boilers = new ArrayList<>();

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<FileData> files = new ArrayList<>();

    public Series() {
        this("null","null", new Kind());
    }

    public Series(String title,String description, Kind kind) {
        this.id = null;
        this.description = description;
        this.kind = kind;
        this.title = title;
    }
}
/*@OneToMany(mappedBy = "series", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImageForSeries> images = new ArrayList<>();*/