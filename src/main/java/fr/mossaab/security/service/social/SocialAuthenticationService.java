package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SocialAuthenticationService {

    private final UserRepository userRepository;
    private final OAuth2UserInfoExtractorFactory extractorFactory;

    public User authenticateOrRegisterVK(String accessToken) {

    }

    public User authenticateOrRegister(String provider, OAuth2User oAuth2User) {
        OAuth2UserInfoExtractor extractor = extractorFactory.getExtractor(provider);
        SocialUserInfo userInfo = extractor.extract(oAuth2User);

        String email = null;
        String firstName = userInfo.getFirstName();
        String lastName = userInfo.getLastName();
        String externalId = userInfo.getId();

        email = provider + "_" + externalId + "@social.user";

        String nickname = (firstName != null ? firstName : "User") + "_" +
                (lastName != null ? lastName : "Social") + "_" +
                externalId;

        // Поиск по email или создание
        String finalEmail = email;
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(finalEmail)
                            .nickname(nickname)
                            .role(Role.USER)
                            .createdAt(LocalDateTime.now())
                            .temporarySecondsBalance(0)
                            .phoneVerified(false)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
