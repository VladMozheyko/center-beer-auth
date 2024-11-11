package fr.mossaab.security.repository;

import fr.mossaab.security.entities.ServiceCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, Long> {
    // Метод для поиска сервисных центров по городу
    List<ServiceCenter> findByCity(String city);

    // Метод для поиска сервисного центра по названию
    Optional<ServiceCenter> findByTitle(String title);
    void deleteById(Long id);
}
