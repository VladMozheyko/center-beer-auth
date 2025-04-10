package fr.mossaab.security.repository;
import fr.mossaab.security.entities.PurchaseMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseMappingRepository extends JpaRepository<PurchaseMapping, Long> {
    Optional<PurchaseMapping> findByPurchaseId(String purchaseId);
}