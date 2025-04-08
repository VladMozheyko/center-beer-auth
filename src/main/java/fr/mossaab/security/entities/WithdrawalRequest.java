package fr.mossaab.security.entities;


import fr.mossaab.security.dto.payment.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private User user;

    @Column(nullable = false)
    private String paymentDetails; // Номер карты или телефона

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;

    private LocalDateTime createdAt;

    @Column(length = 500)
    private String rejectionReason; // ❗ Причина отказа
}
