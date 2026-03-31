package fr.mossaab.security.repository;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationCode(String code);
    Optional<User> findByNickname(String nickname);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.socialAccounts sa WHERE sa.externalId = :externalId AND sa.provider=:provider")
    Optional<User> findBySocialId(String externalId, OAuthProvider provider);

    boolean existsByNickname(String nickname);
}
