package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Question;
import fr.mossaab.security.enums.QuestionCategory;
import fr.mossaab.security.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCategoryAndType(QuestionCategory category, QuestionType type);
}
