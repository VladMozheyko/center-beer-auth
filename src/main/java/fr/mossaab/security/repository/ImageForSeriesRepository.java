package fr.mossaab.security.repository;


import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.ImageForSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageForSeriesRepository extends JpaRepository<ImageForSeries, Long> {
    Optional<ImageForSeries> findByName(String fileName);
}
