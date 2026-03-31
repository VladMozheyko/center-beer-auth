package fr.mossaab.security.service.social;


import fr.mossaab.security.dto.social.SocialUserInfo;

public interface AccessTokenExtractor {
    SocialUserInfo extract(String accessToken);
}
