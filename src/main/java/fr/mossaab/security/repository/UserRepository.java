package fr.mossaab.security.repository;

import fr.mossaab.security.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
