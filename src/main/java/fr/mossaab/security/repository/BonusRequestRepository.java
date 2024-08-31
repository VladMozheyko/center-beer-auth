package fr.mossaab.security.repository;

import fr.mossaab.security.entities.BonusRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BonusRequestRepository extends JpaRepository<BonusRequest, Long> {
    // Вы можете добавить дополнительные методы для поиска по специфическим полям, если это необходимо.
}