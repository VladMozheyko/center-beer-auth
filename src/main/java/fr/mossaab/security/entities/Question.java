package fr.mossaab.security.entities;
import fr.mossaab.security.enums.QuestionCategory;
import fr.mossaab.security.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(
        name = "QUESTIONS",
        indexes = {
                @Index(name = "idx_category_type", columnList = "category, type")
        }
)
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

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private QuestionCategory category;

    @Column(name = "option_a", columnDefinition = "TEXT", nullable = false)
    private String optionA;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private QuestionType type;

    @Column(name = "option_b", columnDefinition = "TEXT", nullable = false)
    private String optionB;

    @Column(name = "option_c", columnDefinition = "TEXT", nullable = false)
    private String optionC;

    @Column(name = "option_d", columnDefinition = "TEXT", nullable = false)
    private String optionD;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;
}

