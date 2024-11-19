package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="boilers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Boiler {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "number", nullable = false, unique = true)
    private Long number;

    @Column(name = "barcode", nullable = false, unique = true)
    private Long barcode;

    @ManyToOne
    @JsonIgnore
    private Series series;

    @ManyToMany(mappedBy = "boilers")
    private List<Value> values =  new ArrayList<>();

}
