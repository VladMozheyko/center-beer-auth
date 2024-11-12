package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Series {

    @Id
    @GeneratedValue
    private Long id;
    private String prefix;     // Префикс, например, "ST"
    private int startRange;    // Начало диапазона, например, 20
    private int endRange;      // Конец диапазона, например, 30
    private String suffix;     // Суффикс, например, "M"

    private String description;

    @ManyToOne
    @JsonIgnore
    private Kind kind;

    @ManyToMany(mappedBy = "seriesList")
    private List<ServiceCenter> serviceCenters = new ArrayList<>();

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

    // Связь с классом Error
    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Error> errors = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "series_passport_titles", // Имя промежуточной таблицы
            joinColumns = @JoinColumn(name = "series_id"),
            inverseJoinColumns = @JoinColumn(name = "passport_title_id")
    )
    private List<BoilerSeriesPassport> boilerSeriesPassports = new ArrayList<>();
}