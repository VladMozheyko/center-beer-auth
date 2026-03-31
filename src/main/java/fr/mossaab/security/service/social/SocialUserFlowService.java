package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialUserFlowService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final OneTimeAuthCodeService oneTimeAuthCodeService;

    public SocialAuthResult analyzeUser(SocialUserInfo userInfo, OAuthProvider provider) {
        // 1. Поиск по socialId (уже ранее привязанная соцсеть)
        Optional<User> userBySocialId = userRepository.findBySocialId(userInfo.getId(), provider);
        if (userBySocialId.isPresent()) {
            return buildResult(userInfo, SocialAuthStatus.SOCIAL_FOUND, "Аккаунт данной соцсети уже связан с пользователем.",
                    userBySocialId.get().getEmail());
        }

        // 2. Поиск среди социальных аккаунтов по email этой соцсети
        List<UserSocialAccount> accountsBySocialEmail = socialAccountRepository.findBySocialEmail(userInfo.getEmail());
        if (accountsBySocialEmail != null && !accountsBySocialEmail.isEmpty()) {
            if (accountsBySocialEmail.size() > 1) {
                // 2.1 Больше одной учётки с этим email — подозрительно, пусть вручную разбираются или специальное сообщение
                return buildResult(userInfo, SocialAuthStatus.ERROR,
                        "Найдено несколько социальных аккаунтов с этим e-mail! Обратитесь в поддержку.",
                        null);
            } else {
                // 2.2 Нашли ровно один social account с этим email
                UserSocialAccount account = accountsBySocialEmail.get(0);
                String userEmail = account.getUser().getEmail();
                return buildResult(
                        userInfo,
                        SocialAuthStatus.NEW_SOCIAL_USER,
                        "Найден пользователь с таким e-mail среди социальных аккаунтов. Предложите привязку соцсети.",
                        userEmail
                );
            }
        }

        // 3. Поиск по основным э-мейлам в базе пользователей
        Optional<User> userByEmail = userRepository.findByEmail(userInfo.getEmail());
        if (userByEmail.isPresent()) {
            // 3.1 Найден пользователь с таким e-mail
            return buildResult(
                    userInfo,
                    SocialAuthStatus.EMAIL_LINKED,
                    "E-mail этой соцсети уже используется в другой учётке. Авторизуйтесь стандартным способом и привяжите соцсеть через профиль.",
                    userByEmail.get().getEmail()
            );
        }

        // 4. Абсолютно новый пользователь
        return buildResult(
                userInfo,
                SocialAuthStatus.NEW_ACCOUNT,
                "Пользователь не найден. Можно продолжить регистрацию через соцсеть.",
                null
        );
    }

    // Вспомогательная функция, создающая объект результата
    private SocialAuthResult buildResult(SocialUserInfo info, SocialAuthStatus status, String message, String baseUserEmail) {
        String authCode = null;
        if (status != SocialAuthStatus.ERROR) {
            authCode = oneTimeAuthCodeService.issueCode(info);
        }
        SocialAuthResult result = new SocialAuthResult();
        result.setStatus(status);
        result.setMessage(message);
        result.setBaseUserEmail(baseUserEmail);
        result.setSocialUser(info);
        result.setAuthCode(authCode);
        return result;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SocialAuthResult {
        private SocialAuthStatus status;
        private String message;
        private String baseUserEmail;
        private SocialUserInfo socialUser;
        private String authCode;
    }

}
