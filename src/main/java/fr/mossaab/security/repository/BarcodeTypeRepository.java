package fr.mossaab.security.repository;

import fr.mossaab.security.entities.BarcodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarcodeTypeRepository extends JpaRepository<BarcodeType, Long> {

    // Поиск по типу и подтипу
    Optional<BarcodeType> findByTypeAndSubtype(String type, String subtype);

    // Проверка существования типа штрихкода по типу и подтипу
    boolean existsByTypeAndSubtype(String type, String subtype);
}
