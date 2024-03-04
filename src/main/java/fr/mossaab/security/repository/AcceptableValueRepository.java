package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Value;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptableValueRepository extends JpaRepository<Value, Long> {
}
