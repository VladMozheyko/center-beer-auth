package fr.mossaab.security.repository;

import fr.mossaab.security.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по электронной почте.
     *
     * @param email Электронная почта пользователя.
     * @return Пользователь, обернутый в Optional, если он существует, иначе пустой Optional.
     */
    Optional<User> findByEmail(String email);

    /**
     * Найти пользователя по коду активации.
     *
     * @param code Код активации.
     * @return Пользователь.
     */
    User findByActivationCode(String code);

    /**
     * Проверить, существует ли пользователь с данной электронной почтой.
     *
     * @param email Электронная почта пользователя.
     * @return true, если пользователь существует, иначе false.
     */
    boolean existsByEmail(String email);
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.refreshTokens")
    List<User> findAllWithRefreshTokens();
}
