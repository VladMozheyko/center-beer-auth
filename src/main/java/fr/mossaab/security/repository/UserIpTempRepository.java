package fr.mossaab.security.repository;

import fr.mossaab.security.entities.UserIpTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UserIpTempRepository extends JpaRepository<UserIpTemp, Long> {

    /**
     * Находим все записи по userId отсортированные по убыванию даты создания (от новых к старым)
     */
    List<UserIpTemp> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM UserIpTemp u WHERE u.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
