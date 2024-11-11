package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="acceptable_value")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Value {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Characteristic characteristic;

    private String sValue;
    private Double dValue;
    private Double minValue;
    private Double maxValue;

    @ManyToMany
    @JsonIgnore
    private List<Boiler> boilers =  new ArrayList<>();
}
