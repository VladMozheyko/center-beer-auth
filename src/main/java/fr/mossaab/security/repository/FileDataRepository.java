package fr.mossaab.security.repository;

import fr.mossaab.security.entities.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface FileDataRepository extends JpaRepository<FileData,Long> {
    Optional<FileData> findByName(String fileName);
    @Query("SELECT f FROM FileData f WHERE f.user.id = :userId")
    Optional<FileData> findByUserId(@Param("userId") Long userId);
    @Modifying
    @Transactional
    void deleteByName(String fileName);
}
