package fr.mossaab.security.repository;

import fr.mossaab.security.entities.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
}
