package fr.mossaab.security.repository;

import fr.mossaab.security.entities.ServiceCentre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceCentreRepository extends JpaRepository<ServiceCentre, Integer> {
    void deleteById(int id);
}
