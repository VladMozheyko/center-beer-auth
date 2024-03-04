package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="acceptable_value")
@Setter
@Getter
public class Value {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private Characteristic characteristic;

    private String sValue;
    private Double dValue;
    private Double minValue;
    private Double maxValue;

    @ManyToMany
    @JsonIgnore
    private List<Boiler> boilers =  new ArrayList<>();

    // доп конструкторы
    public Value(Characteristic characteristic, Double dValue){
        this(characteristic, "null", dValue, 0.0, 0.0);
    }

    public Value(Characteristic characteristic, String sValue){
        this(characteristic, sValue, 0.0, 0.0, 0.0);
    }

    public Value(Characteristic characteristic, Double minValue, Double maxValue){
        this(characteristic, "null", 0.0, minValue, maxValue);
    }

    public Value() {
        this(new Characteristic(), "null", 0.0, 0.0, 0.0);
    }

    public Value(Characteristic characteristic, String sValue, Double dValue, Double minValue, Double maxValue) {
        this.id = null;
        this.characteristic = characteristic;
        this.sValue = sValue;
        this.dValue = dValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}
