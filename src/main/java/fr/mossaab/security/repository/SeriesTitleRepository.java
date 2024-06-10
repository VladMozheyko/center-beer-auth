package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.SeriesTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesTitleRepository extends JpaRepository<SeriesTitle, Integer> {
}