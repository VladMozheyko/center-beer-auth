package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Error;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<Error,Long> {
}
