package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValueRepository extends JpaRepository<Value, Long> {
    // Additional query methods can be defined here if needed
    // Method to find all values within a specified range of minValue and maxValue
    //List<Value> findByMinValueGreaterThanEqualAndMaxValueLessThanEqual(Double minValue, Double maxValue);

    // Method to find values by exact dValue
    //List<Value> findByDValue(Double dValue);

}
