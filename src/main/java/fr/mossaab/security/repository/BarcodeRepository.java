package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Barcode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarcodeRepository extends JpaRepository<Barcode, Long> {
    // Дополнительные методы поиска можно добавить здесь, если потребуется

    Optional<Barcode> findByCode(Long code);

    boolean existsByCode(Long code);
}
