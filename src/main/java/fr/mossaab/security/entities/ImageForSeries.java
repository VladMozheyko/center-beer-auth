package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name="images_for_series")
@Setter
@Getter
public class ImageForSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String filePath;

    @ManyToOne
    @JsonIgnore
    private Series series;

    public ImageForSeries() {
        this("null", "null", "null", new Series());
    }

    public ImageForSeries(String name, String type, String filePath, Series series) {
        this.id = null;
        this.name = name;
        this.type = type;
        this.filePath = filePath;
        this.series = series;
    }
}
