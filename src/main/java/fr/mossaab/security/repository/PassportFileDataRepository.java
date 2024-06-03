package fr.mossaab.security.repository;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.PassportFileData;
import fr.mossaab.security.entities.PassportTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassportFileDataRepository extends JpaRepository<PassportFileData, Long> {
    Optional<PassportFileData> findByName(String fileName);
    List<PassportFileData> findByPassportTitle(PassportTitle passportTitle);
    // Дополнительные методы поиска (если нужны) можно объявить здесь
}
