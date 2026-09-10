package fr.mossaab.security.repository;

import fr.mossaab.security.entities.UserIpTemp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserIpTempRepository extends JpaRepository<UserIpTemp, Long> {

    /**
     * Находим все записи по userId по убыванию даты создания (от новых к старым)
     */
    List<UserIpTemp> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
