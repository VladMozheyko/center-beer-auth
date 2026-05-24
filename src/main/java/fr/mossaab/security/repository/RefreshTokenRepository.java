package fr.mossaab.security.repository;

import fr.mossaab.security.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с токенами обновления.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Найти токен обновления по значению токена.
     *
     * @param token Значение токена.
     * @return Токен обновления, обернутый в Optional, если он существует, иначе пустой Optional.
     */
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdAndExpiryDateAfter(Long user_id, Instant expiryDate);

    void deleteByToken(String token);

    void deleteByUserId(Long userId);

    //получить все refresh-токены пользователя (например, для списка устройств)
    List<RefreshToken> findByUserId(Long userId);

    //один активный токен для пользователя на конкретном устройстве
    Optional<RefreshToken> findByUserIdAndDeviceIdAndExpiryDateAfter(Long userId,
                                                                     String deviceId,

                                                                     Instant now);

    @Query("SELECT rt.deviceId FROM RefreshToken rt WHERE rt.token = :refreshToken")
    String findDeviceIdByToken(@Param("refreshToken") String refreshToken);

    @Modifying
    @Query("DELETE FROM RefreshToken rt " +
            "WHERE rt.user.id = :userId " +
            "AND rt.token <> :refreshToken")
    void deleteByUserIdWhereNotThisToken(@Param("userId") Long userId, @Param("refreshToken") String refreshToken);
}
