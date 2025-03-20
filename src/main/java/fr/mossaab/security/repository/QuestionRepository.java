package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Question;
import fr.mossaab.security.enums.QuestionCategory;
import fr.mossaab.security.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT q FROM Question q WHERE q.category = :category AND q.type = :type")
    List<Question> findByCategoryAndType(
            @Param("category") QuestionCategory category,
            @Param("type") QuestionType type
    );
}
