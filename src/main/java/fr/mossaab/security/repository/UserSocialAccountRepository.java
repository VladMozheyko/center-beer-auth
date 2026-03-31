package fr.mossaab.security.repository;

import fr.mossaab.security.entities.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    List<UserSocialAccount> findBySocialEmail(String email);
}
