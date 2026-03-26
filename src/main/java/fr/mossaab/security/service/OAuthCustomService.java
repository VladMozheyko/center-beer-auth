package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthCustomService {

    private final UserRepository userRepository;

    public User findOrCreateOAuthUser(String externalId, OAuthProvider provider, String email, String firstName, String lastName) {
        User user;
        // 1. Поиск по social externalId
        user = this.findUserByExternalID(externalId, provider);

//        // 2. Если не нашли, ищем по email
//        if (user == null) {
//            user = findUserByEmail(email, provider, externalId);
//        }

        // 3. Если user новый, создаём

        if (user == null) {
            user = User.builder()
                    .email(generateFakeEmail(provider, externalId))
                    .nickname(getRandomNickname(firstName, lastName, email))
                    .role(Role.USER)
                    .createdAt(LocalDateTime.now())
                    .temporarySecondsBalance(0)
                    .phoneVerified(false)
                    .build();

            UserSocialAccount socialAcc = UserSocialAccount.builder()
                    .externalId(externalId)
                    .provider(provider)
                    .socialEmail(email)
                    .user(user)
                    .build();

            Set<UserSocialAccount> socialList = new HashSet<>();
            socialList.add(socialAcc);
            user.setSocialAccounts(socialList);

            userRepository.save(user);
        }
        return user;
    }

//    public User findUserByEmail(String email, OAuthProvider provider, String externalId) {
//        User user = null;
//        if (email != null && !email.isEmpty()) {
//            user = userRepository.findByEmail(email).orElse(null);
//            if (user != null && provider != OAuthProvider.DEFAULT) {
//                // Привязываем id соцсети к найденному пользователю
//                switch (provider) {
//                    case VK -> {
//                        if (user.getVkId() == null) user.setVkId(externalId);
//                    }
//                    case GOOGLE -> {
//                        if (user.getGoogleId() == null) user.setGoogleId(externalId);
//                    }
//                    case YANDEX -> {
//                        if (user.getYandexId() == null) user.setYandexId(externalId);
//                    }
//                }
//                userRepository.save(user);
//            }
//        }
//        return user;
//    }

    public User findUserByExternalID(String externalId, OAuthProvider provider) {
        User user = null;
        if (provider != OAuthProvider.DEFAULT && externalId != null) {
            switch (provider) {
                case VK -> user = userRepository.findBySocialId(externalId, OAuthProvider.VK).orElse(null);
                case GOOGLE -> user = userRepository.findBySocialId(externalId, OAuthProvider.GOOGLE).orElse(null);
                case YANDEX -> user = userRepository.findBySocialId(externalId, OAuthProvider.YANDEX).orElse(null);
            }
        }
        return user;
    }

    private String generateFakeEmail(OAuthProvider provider, String externalId) {
        return provider.name().toLowerCase() +
                "_" + externalId +
                "@" + "no-email.com";
    }

    private String getRandomNickname(String firstName, String lastName, String email) {
        String randomPart = firstName + "_" + lastName + "_" + UUID.randomUUID();
        if (lastName == null && firstName == null) {
            randomPart = email;
        }
        if (lastName == null && firstName == null && email == null) {
            randomPart = "user_" + UUID.randomUUID() + "_" + UUID.randomUUID();
        }
        if (randomPart.length() > 50) randomPart = randomPart.substring(0, 50);
        return randomPart;
    }

    public User addAccount(User user, String externalId, OAuthProvider provider, String email) {

        UserSocialAccount socialAcc = UserSocialAccount.builder()
                .externalId(externalId)
                .provider(provider)
                .socialEmail(email)
                .user(user)
                .build();
        Set<UserSocialAccount> listAccounts = user.getSocialAccounts() == null ? new HashSet<>() : user.getSocialAccounts();
        listAccounts.add(socialAcc);
        user.setSocialAccounts(listAccounts);
        userRepository.saveAndFlush(user);
        return user;
    }
}
