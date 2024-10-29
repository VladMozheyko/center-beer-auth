package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "barcode_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeType {

    @Id
    @GeneratedValue()
    private Long id;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "subtype", nullable = false)
    private String subtype;


    @OneToMany(mappedBy = "barcodeType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Barcode> barcodes = new ArrayList<>(); // Изменили Set на List

}