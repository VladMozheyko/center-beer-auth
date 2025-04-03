package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String productId;
    private String transactionId;

    private BigDecimal amount;

    private boolean confirmed;

    private LocalDateTime createdAt = LocalDateTime.now();
}
