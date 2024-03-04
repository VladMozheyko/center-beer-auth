package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Kind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KindRepository extends JpaRepository<Kind, Long> {

    // получить виды и их сериии по типу
    List<Kind> getKindByTypeId(Long id);
}
