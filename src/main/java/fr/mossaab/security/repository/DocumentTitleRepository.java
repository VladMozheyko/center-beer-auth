package fr.mossaab.security.repository;

import fr.mossaab.security.entities.DocumentCategory;
import fr.mossaab.security.entities.DocumentTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentTitleRepository extends JpaRepository<DocumentTitle, Integer> {
    List<DocumentTitle> findAllByCategory(DocumentCategory category);
}

