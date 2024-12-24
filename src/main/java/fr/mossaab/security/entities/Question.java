package fr.mossaab.security.entities;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "QUESTIONS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Question {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Текст вопроса.
     */
    @Column(nullable = false)
    private String text;

    /**
     * Вариант ответа A.
     */
    @Column(name = "option_a", nullable = false)
    private String optionA;

    /**
     * Вариант ответа B.
     */
    @Column(name = "option_b", nullable = false)
    private String optionB;

    /**
     * Вариант ответа C.
     */
    @Column(name = "option_c", nullable = false)
    private String optionC;

    /**
     * Вариант ответа D.
     */
    @Column(name = "option_d", nullable = false)
    private String optionD;

    /**
     * Правильный ответ (например, "A", "B", "C", "D").
     */
    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

}
