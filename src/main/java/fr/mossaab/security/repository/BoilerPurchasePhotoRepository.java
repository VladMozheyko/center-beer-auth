package fr.mossaab.security.repository;


import fr.mossaab.security.entities.BoilerPurchasePhoto;
import fr.mossaab.security.entities.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BoilerPurchasePhotoRepository extends JpaRepository<BoilerPurchasePhoto,Long> {
    Optional<BoilerPurchasePhoto> findByName(String fileName);
    @Modifying
    @Transactional
    void deleteByName(String fileName);
}