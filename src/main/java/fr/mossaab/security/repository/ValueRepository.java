package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValueRepository extends JpaRepository<Value, Long> {
    // Additional query methods can be defined here if needed
}
