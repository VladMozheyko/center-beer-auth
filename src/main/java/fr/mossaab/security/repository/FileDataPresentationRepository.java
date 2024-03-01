package fr.mossaab.security.repository;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.FileDataPresentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface FileDataPresentationRepository extends JpaRepository<FileDataPresentation,Long> {
    Optional<FileDataPresentation> findByName(String fileName);
}
