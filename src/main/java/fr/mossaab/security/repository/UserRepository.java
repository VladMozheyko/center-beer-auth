package fr.mossaab.security.repository;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // Методы для работы со статусами
    Optional<User> findByNicknameAndStatus(String nickname, UserStatus status);
    Optional<User> findByEmailAndStatus(String email, UserStatus status);
    List<User> findByStatusAndCreatedAtBefore(UserStatus status, LocalDateTime before);
    long countByStatus(UserStatus status);

    @Modifying
    @Query("DELETE FROM User u WHERE u.status = :status AND u.createdAt < :before")
    int deleteByStatusAndCreatedAtBefore(@Param("status") UserStatus status, @Param("before") LocalDateTime before);
}
