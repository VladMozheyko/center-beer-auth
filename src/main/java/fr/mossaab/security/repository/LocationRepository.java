package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
