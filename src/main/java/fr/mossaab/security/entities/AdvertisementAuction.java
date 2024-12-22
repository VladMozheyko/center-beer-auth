package fr.mossaab.security.entities;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "advertisement_auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertisementAuction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Реклама, выигравшая аукцион.
     */
    @OneToOne
    @JoinColumn(name = "advertisement_id")
    private Advertisement currentAdvertisement;

    /**
     * Текущая максимальная ставка в груши.
     */
    @Column(nullable = false)
    private Integer currentBid;

    /**
     * Пользователь, сделавший максимальную ставку.
     */

    /**
     * Статус аукциона.
     */
    @Column(nullable = false)
    private String status; // Например, "ACTIVE", "COMPLETED"
}
