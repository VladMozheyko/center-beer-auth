package fr.mossaab.security.repository;

import fr.mossaab.security.entities.PassportCategory;
import fr.mossaab.security.entities.PassportTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassportTitleRepository extends JpaRepository<PassportTitle, Integer> {
    // Дополнительные методы поиска (если нужны) можно объявить здесь
    List<PassportTitle> findAllByCategory(PassportCategory category);
}
