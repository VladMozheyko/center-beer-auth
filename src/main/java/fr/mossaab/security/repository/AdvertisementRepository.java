package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    // Можно добавить дополнительные методы, если нужно
}