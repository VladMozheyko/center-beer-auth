package fr.mossaab.security.repository;

import fr.mossaab.security.entities.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    List<UserSocialAccount> findBySocialEmail(String email);

    @Query("SELECT u FROM UserSocialAccount u WHERE u.user.id = :id")
    List<UserSocialAccount> findByUserId(Long id);
}
