package fr.mossaab.security.entities;

import fr.mossaab.security.enums.PaymentMethod;
import fr.mossaab.security.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionId; // Уникальный ID транзакции (например, UUID)

    @Column(nullable = false)
    private Double amount; // Сумма транзакции

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status; // Статус транзакции: PENDING, COMPLETED, FAILED, CANCELED

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // Метод оплаты: SBP, CARD, etc.

    @Column(nullable = false)
    private String currency; // Валюта транзакции (например, RUB, USD)

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime createdAt; // Время создания транзакции

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // Время последнего обновления транзакции

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Ссылка на пользователя, совершившего транзакцию
}
