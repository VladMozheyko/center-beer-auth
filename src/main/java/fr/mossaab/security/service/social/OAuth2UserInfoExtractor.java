package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserInfoExtractor {
    SocialUserInfo extract(OAuth2User oAuth2User, String accessToken);
}
