package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "purchase_mappings", uniqueConstraints = @UniqueConstraint(columnNames = "purchaseId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String purchaseId;

    private Long userId;
}