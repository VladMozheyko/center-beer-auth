package fr.mossaab.security.repository;

import fr.mossaab.security.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
