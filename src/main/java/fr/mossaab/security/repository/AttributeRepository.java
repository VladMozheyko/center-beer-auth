package fr.mossaab.security.repository;


import fr.mossaab.security.entities.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository  extends JpaRepository<Attribute, Long> {
}
