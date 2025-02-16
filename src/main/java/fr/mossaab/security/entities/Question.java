package fr.mossaab.security.entities;
import fr.mossaab.security.enums.QuestionCategory;
import fr.mossaab.security.enums.QuestionType;
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
    @Column(name = "text", length = 1000, nullable = false)
    private String text;

    @Enumerated(EnumType.STRING) // Сохраняем название enum в БД в виде строки
    @Column(name = "category", nullable = false)
    private QuestionCategory category;
    /**
     * Вариант ответа A.
     */
    @Column(name = "option_a", nullable = false)
    private String optionA;
    /**
     * Новый вид вопроса — русский или английский.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private QuestionType type;
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
