package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "boiler_purchase")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoilerPurchasePhoto {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Имя файла.
     */
    private String name;

    /**
     * Тип файла.
     */
    private String type;

    /**
     * Путь к файлу.
     */
    private String filePath;
    /**
     * Родительская сущность BonusRequest.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bonus_request_id", nullable = false)
    @JsonBackReference
    private BonusRequest bonusRequest;

}