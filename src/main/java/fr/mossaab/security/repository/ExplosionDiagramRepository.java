package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.ExplosionDiagram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExplosionDiagramRepository extends JpaRepository<ExplosionDiagram,Long>  {
}
