package fr.mossaab.security.service.social.service;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;

/**
 * Сервис для привязки социального аккаунта к существующему пользователю.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAccountLinkingService {

    private final UserRepository userRepository;

    public void linkSocialAccount(User user, SocialUserInfo info, OAuthProvider provider) {
        log.info("[LINK ACCOUNT] - Процесс привязки аккаунта, текущий email:{}, email привязываемый: {}", user.getEmail(), info.getEmail());
        UserSocialAccount socialAcc = UserSocialAccount.builder()
                .externalId(info.getId())
                .provider(provider)
                .socialEmail(info.getEmail())
                .user(user)
                .build();

        if (user.getSocialAccounts() == null) {
            user.setSocialAccounts(new HashSet<>());
        }
        user.getSocialAccounts().add(socialAcc);

        userRepository.saveAndFlush(user);
        log.info("[LINK ACCOUNT] - Социальный аккаунт привязан успешно: {} -> {}", provider, user.getEmail());
    }
}
