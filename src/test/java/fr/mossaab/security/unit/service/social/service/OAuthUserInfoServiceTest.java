package fr.mossaab.security.unit.service.social.service;

import static org.junit.jupiter.api.Assertions.*;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.service.social.extractor.UserInfoExtractor;
import fr.mossaab.security.service.social.factory.UserInfoExtractorFactory;
import fr.mossaab.security.service.social.service.OAuthUserInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthUserInfoServiceTest {

    @InjectMocks
    private OAuthUserInfoService userInfoService;

    @Mock
    private UserInfoExtractorFactory extractorFactory;

    @Mock
    private UserInfoExtractor extractor;

    private OAuth2User oAuth2User;
    private SocialUserInfo expectedSocialUserInfo;

    @BeforeEach
    void setUp() {
        // Инициализация примера данных пользователя
        oAuth2User = mock(OAuth2User.class);
        expectedSocialUserInfo = new SocialUserInfo();
        expectedSocialUserInfo.setId("test-id");
        expectedSocialUserInfo.setEmail("test@example.com");

        when(extractorFactory.getExtractor(OAuthProvider.GOOGLE)).thenReturn(extractor);
        when(extractor.extract(oAuth2User, "test-access-token")).thenReturn(expectedSocialUserInfo);
    }

    @Test
    @DisplayName("Should fetch user info successfully for Google provider")
    void getUserInfo_fromGoogle() {
        SocialUserInfo result = userInfoService.getUserInfo(oAuth2User, "test-access-token", OAuthProvider.GOOGLE);

        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("test@example.com", result.getEmail());

        // Проверка того, что метод extract вызывается с нужными параметрами
        verify(extractor).extract(eq(oAuth2User), eq("test-access-token"));
    }
}