package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TypeRepository extends JpaRepository<Type, Long> {
    Optional<Type> findByTitle(String title);
}
