package fr.mossaab.security.repository;

import fr.mossaab.security.entities.BoilerSeriesPassport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoilerSeriesPassportRepository extends JpaRepository<BoilerSeriesPassport, Long> {
    // Дополнительные методы поиска (если нужны) можно объявить здесь
    //List<BoilerSeriesPassport> findAllByCategory(PassportCategory category);
}
