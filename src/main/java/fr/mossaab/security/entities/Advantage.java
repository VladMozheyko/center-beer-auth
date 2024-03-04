package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.mossaab.security.enums.CategoryOfAdvantage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "advantages")
@Setter
@Getter
public class Advantage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "icon_path")
    private String iconPath;

    @Enumerated(EnumType.STRING)
    private CategoryOfAdvantage category;

    @ManyToMany
    @JsonIgnore
    private List<Series> series = new ArrayList<>();

    public Advantage() {
        this("null", "null", CategoryOfAdvantage.COMFORT);
    }

    public Advantage(String title, String iconPath, CategoryOfAdvantage category) {
        this.id = null;
        this.title = title;
        this.iconPath = iconPath;
        this.category = category;
    }
}
