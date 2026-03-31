package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;

    public User createAccount(OAuthProvider provider, SocialUserInfo userInfo) {

        User user = User.builder()
                .email(userInfo.getEmail() == null ? generateFakeEmail(provider, userInfo.getId()) : userInfo.getEmail())
                .nickname(getRandomNickname(userInfo.getFirstName(), userInfo.getLastName(), userInfo.getEmail()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .temporarySecondsBalance(0)
                .phoneVerified(false)
                .build();

        UserSocialAccount socialAcc = UserSocialAccount.builder()
                .externalId(userInfo.getId())
                .provider(provider)
                .socialEmail(userInfo.getEmail())
                .user(user)
                .build();

        Set<UserSocialAccount> socialList = new HashSet<>();
        socialList.add(socialAcc);
        user.setSocialAccounts(socialList);

        userRepository.save(user);
        log.debug("Пользователь создан: {}", user);
        return user;
    }

    public void addAccount(User user, SocialUserInfo info, OAuthProvider provider) {

        UserSocialAccount socialAcc = UserSocialAccount.builder()
                .externalId(info.getId())
                .provider(provider)
                .socialEmail(info.getEmail())
                .user(user)
                .build();
        Set<UserSocialAccount> listAccounts = user.getSocialAccounts() == null ? new HashSet<>() : user.getSocialAccounts();
        listAccounts.add(socialAcc);
        user.setSocialAccounts(listAccounts);
        userRepository.saveAndFlush(user);
        log.debug("Пользватель {}, добавление аккаунта с сервиса {}", user, socialAcc);
    }

    private String generateFakeEmail(OAuthProvider provider, String externalId) {
        return provider.name().toLowerCase() +
                "_" + externalId +
                "@" + "no-email.com";
    }

    private String getRandomNickname(String firstName, String lastName, String email) {
        // 1. Создаём базовую часть из имени и фамилии (транслит)
        StringBuilder base = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            base.append(transliterateCyrillic(firstName.trim())).append("_");
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            base.append(transliterateCyrillic(lastName.trim()));
        }

        String baseName = base.toString().replaceAll("_+$", ""); // убираем последнее подчёркивание

        // 2. Если имя и фамилия пусты — пробуем взять часть от email
        if (baseName.isEmpty() && email != null && !email.isBlank()) {
            String localPart = email.split("@", 2)[0];
            baseName = transliterateCyrillic(localPart).replaceAll("[^a-zA-Z0-9]", "_");
        }

        // 3. Если всё равно пусто — используем "user"
        if (baseName.isEmpty()) {
            baseName = "user";
        }

        // 4. Ограничиваем длину базы, чтобы с суффиксом не выйти за 50
        int maxBaseLength = 40;
        if (baseName.length() > maxBaseLength) {
            baseName = baseName.substring(0, maxBaseLength);
        }

        // 5. Генерируем уникальный ник с числовым суффиксом
        String nickname = baseName;
        int suffix = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseName + "_" + (suffix++);
            if (nickname.length() > 50) {
                // Если слишком длинный — обрезаем базу и пробуем снова
                int allowedLength = 50 - (String.valueOf(suffix)).length() - 1;
                baseName = baseName.substring(0, allowedLength);
                nickname = baseName + "_" + (suffix++);
            }
        }

        return nickname;
    }

    // Метод транслитерации кириллицы в латиницу
    private String transliterateCyrillic(String input) {
        if (input == null || input.isEmpty()) return "";

        Map<Character, String> map = new HashMap<>();
        map.put('а', "a");
        map.put('б', "b");
        map.put('в', "v");
        map.put('г', "g");
        map.put('д', "d");
        map.put('е', "e");
        map.put('ё', "e");
        map.put('ж', "zh");
        map.put('з', "z");
        map.put('и', "i");
        map.put('й', "y");
        map.put('к', "k");
        map.put('л', "l");
        map.put('м', "m");
        map.put('н', "n");
        map.put('о', "o");
        map.put('п', "p");
        map.put('р', "r");
        map.put('с', "s");
        map.put('т', "t");
        map.put('у', "u");
        map.put('ф', "f");
        map.put('х', "h");
        map.put('ц', "ts");
        map.put('ч', "ch");
        map.put('ш', "sh");
        map.put('щ', "sch");
        map.put('ъ', "");
        map.put('ы', "y");
        map.put('ь', "");
        map.put('э', "e");
        map.put('ю', "yu");
        map.put('я', "ya");

        StringBuilder output = new StringBuilder();
        for (char c : input.toCharArray()) {
            output.append(map.getOrDefault(Character.toLowerCase(c), String.valueOf(c)));
        }
        return output.toString()
                .replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
