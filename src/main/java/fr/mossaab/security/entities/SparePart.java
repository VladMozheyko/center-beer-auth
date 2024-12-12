package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SPARE_PART")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SparePart {

    /**
     * Уникальный идентификатор запчасти.
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Артикул запчасти.
     */
    @Column(nullable = false, unique = true)
    private String articleNumber;

    /**
     * Наименование запчасти.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Цена АСЦ в юанях (¥).
     */
    @Column(name = "asc_price_yuan", precision = 15, scale = 2)
    private BigDecimal ascPriceYuan;

    /**
     * Цена ОПТ в юанях (¥).
     */
    @Column(name = "wholesale_price_yuan", precision = 15, scale = 2)
    private BigDecimal wholesalePriceYuan;

    /**
     * РРЦ в юанях (¥).
     */
    @Column(name = "retail_price_yuan", precision = 15, scale = 2)
    private BigDecimal retailPriceYuan;

    /**
     * Цена АСЦ в рублях (₽).
     */
    @Column(name = "asc_price_rub", precision = 15, scale = 2)
    private BigDecimal ascPriceRub;

    /**
     * Цена ОПТ в рублях (₽).
     */
    @Column(name = "wholesale_price_rub", precision = 15, scale = 2)
    private BigDecimal wholesalePriceRub;

    /**
     * РРЦ в рублях (₽).
     */
    @Column(name = "retail_price_rub", precision = 15, scale = 2)
    private BigDecimal retailPriceRub;

    /**
     * Связь с данными файла (FileData) один-к-одному.
     */
    @OneToOne(optional = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "file_data_id", referencedColumnName = "id")
    private FileData fileData;

    @ManyToOne
    @JoinColumn(name = "explosion_diagram_id")
    private ExplosionDiagram explosionDiagram;
    @ManyToMany
    @JoinTable(
            name = "spare_part_boiler",
            joinColumns = @JoinColumn(name = "spare_part_id"),
            inverseJoinColumns = @JoinColumn(name = "boiler_id")
    )
    private List<Boiler> boilers = new ArrayList<>();
}
