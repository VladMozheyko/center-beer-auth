package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "barcode")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Barcode {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private Long code;

    @Column(name = "used", nullable = false)
    private boolean used = false;
    @OneToOne(mappedBy = "barcode", cascade = CascadeType.ALL, orphanRemoval = true)
    private BonusRequest bonusRequest;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barcode_type_id", nullable = false)
    private BarcodeType barcodeType;
}
