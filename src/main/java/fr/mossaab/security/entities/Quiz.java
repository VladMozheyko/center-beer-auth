package fr.mossaab.security.entities;

import fr.mossaab.security.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
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
    private Integer duration = 60;
    /**
     * Текущий статус викторины.
     */
    @Column(nullable = false)
    private String status; // Например, "ACTIVE", "COMPLETED", "PENDING"

    @Column(nullable = false)
    private Integer totalPoints = 0;

    @Transient
    public Integer getRemainingTime() {
        if (status.equals("COMPLETED")) {
            return 0; // Если викторина завершена, оставшееся время равно 0
        }
        LocalDateTime endTime = startTime.plusMinutes(duration);
        long minutesLeft = Duration.between(LocalDateTime.now(), endTime).toMinutes();
        return minutesLeft > 0 ? (int) minutesLeft : 0;
    }
}
