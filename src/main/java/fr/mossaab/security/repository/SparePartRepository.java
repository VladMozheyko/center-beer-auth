package fr.mossaab.security.repository;

import fr.mossaab.security.entities.ServiceCenter;
import fr.mossaab.security.entities.SparePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SparePartRepository extends JpaRepository<SparePart, Long> {
    // Метод для поиска запчасти по артикулу (артикул уникален, поэтому используем Optional)
    Optional<SparePart> findByArticleNumber(String articleNumber);

    // Метод для поиска запчастей по наименованию (могут быть несколько запчастей с одинаковым наименованием)
    List<SparePart> findByName(String name);
}
