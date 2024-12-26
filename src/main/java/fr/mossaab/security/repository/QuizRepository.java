package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // Если необходимо, можно добавить дополнительные методы
}
