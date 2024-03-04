package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="kinds")
@Getter
@Setter
public class Kind {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    @JsonIgnore
    private Type type;

    @OneToMany(mappedBy = "kind", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Series> series = new ArrayList<>();

    public Kind() {
        this("null", "null", new Type());
    }

    public Kind(String title, String description, Type type) {
        this.id = null;
        this.title = title;
        this.description = description;
        this.type = type;
    }
}
