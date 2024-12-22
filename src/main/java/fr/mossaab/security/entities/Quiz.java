package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Время начала викторины.
     */
    @Column(nullable = false)
    private LocalDateTime startTime;

    /**
     * Длительность викторины в минутах.
     */
    @Column(nullable = false)
    private Integer duration;

    /**
     * Текущий статус викторины.
     */
    @Column(nullable = false)
    private String status; // Например, "ACTIVE", "COMPLETED", "PENDING"

    /**
     * Пользователь-победитель викторины.
     */

    /**
     * Общий пул баллов.
     */
    @Column(nullable = false)
    private Integer totalPoints = 0;
}
