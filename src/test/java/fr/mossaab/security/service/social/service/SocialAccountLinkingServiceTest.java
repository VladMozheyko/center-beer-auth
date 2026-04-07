package fr.mossaab.security.service.social.service;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты для SocialAccountLinkingService")
class SocialAccountLinkingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SocialAccountLinkingService socialAccountLinkingService;

    @Nested
    @DisplayName("Тестирование метода linkSocialAccount")
    class LinkSocialAccountTests {

        @Test
        @DisplayName("У пользователя уже есть множество socialAccounts — новый аккаунт добавляется")
        void linkSocialAccount_UserHasExistingSet_NewAccountAdded() {
            // given
            User user = User.builder()
                    .email("user@example.com")
                    .socialAccounts(new HashSet<>())
                    .build();

            SocialUserInfo info = new SocialUserInfo();
            info.setId("social-id-123");
            info.setEmail("social@example.com");

            OAuthProvider provider = OAuthProvider.GOOGLE;

            // when
            socialAccountLinkingService.linkSocialAccount(user, info, provider);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertNotNull(savedUser.getSocialAccounts());
            assertEquals(1, savedUser.getSocialAccounts().size());

            UserSocialAccount acc = savedUser.getSocialAccounts().iterator().next();
            assertEquals("social-id-123", acc.getExternalId());
            assertEquals("social@example.com", acc.getSocialEmail());
            assertEquals(provider, acc.getProvider());
            assertSame(savedUser, acc.getUser());
        }

        @Test
        @DisplayName("У пользователя socialAccounts == null — множество инициализируется и аккаунт добавляется")
        void linkSocialAccount_UserHasNullSet_SetInitializedAndAccountAdded() {
            // given
            User user = User.builder()
                    .email("user2@example.com")
                    .socialAccounts(null)
                    .build();

            SocialUserInfo info = new SocialUserInfo();
            info.setId("social-id-456");
            info.setEmail("another-social@example.com");

            OAuthProvider provider = OAuthProvider.YANDEX;

            // when
            socialAccountLinkingService.linkSocialAccount(user, info, provider);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertNotNull(savedUser.getSocialAccounts());
            assertEquals(1, savedUser.getSocialAccounts().size());

            UserSocialAccount acc = savedUser.getSocialAccounts().iterator().next();
            assertEquals("social-id-456", acc.getExternalId());
            assertEquals("another-social@example.com", acc.getSocialEmail());
            assertEquals(provider, acc.getProvider());
            assertSame(savedUser, acc.getUser());
        }
    }
}