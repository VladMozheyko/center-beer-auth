package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Presentation;
import fr.mossaab.security.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresentationRepository extends JpaRepository<Presentation, Long> {
}
