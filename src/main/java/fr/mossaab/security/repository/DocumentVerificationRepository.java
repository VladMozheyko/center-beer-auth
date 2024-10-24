package fr.mossaab.security.repository;

import fr.mossaab.security.entities.DocumentVerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DocumentVerificationRepository extends JpaRepository<DocumentVerificationRequest, Long> {
}
