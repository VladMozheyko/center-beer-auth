package fr.mossaab.security.service.social.service;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.SocialAuthStatus;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("Unit-тесты для SocialUserFlowService")
class SocialUserFlowServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSocialAccountRepository socialAccountRepository;

    @Mock
    private OneTimeAuthCodeService oneTimeAuthCodeService;

    @InjectMocks
    private SocialUserFlowService socialUserFlowService;

    private SocialUserInfo buildUserInfo(String id, String email) {
        SocialUserInfo info = new SocialUserInfo();
        info.setId(id);
        info.setEmail(email);
        return info;
    }

    @Nested
    @DisplayName("analyzeUser: поиск по socialId")
    class AnalyzeUserBySocialId {

        @Test
        @DisplayName("Уже привязанный socialId -> SOCIAL_FOUND и выдача одноразового кода")
        void analyzeUser_SocialIdFound_ReturnsSocialFound() {
            // given
            SocialUserInfo info = buildUserInfo("social-123", "social@example.com");
            OAuthProvider provider = OAuthProvider.GOOGLE;

            User existingUser = User.builder()
                    .email("user@example.com")
                    .build();

            when(userRepository.findBySocialId("social-123", provider))
                    .thenReturn(Optional.of(existingUser));
            when(oneTimeAuthCodeService.issueCode(info))
                    .thenReturn("auth-code-1");

            // when
            SocialUserFlowService.SocialAuthResult result =
                    socialUserFlowService.analyzeUser(info, provider);

            // then
            assertEquals(SocialAuthStatus.SOCIAL_FOUND, result.getStatus());
            assertEquals("Аккаунт данной соцсети уже связан с пользователем.", result.getMessage());
            assertEquals("user@example.com", result.getBaseUserEmail());
            assertEquals(info, result.getSocialUser());
            assertEquals("auth-code-1", result.getAuthCode());

            verify(userRepository, times(1)).findBySocialId("social-123", provider);
            verify(oneTimeAuthCodeService, times(1)).issueCode(info);
            verifyNoMoreInteractions(userRepository, socialAccountRepository, oneTimeAuthCodeService);
        }
    }

    @Nested
    @DisplayName("analyzeUser: поиск по социальным аккаунтам по email соцсети")
    class AnalyzeUserBySocialEmail {

        @Test
        @DisplayName("Несколько social-аккаунтов с одним email -> ERROR, без выдачи кода")
        void analyzeUser_MultipleSocialAccountsByEmail_ReturnsError() {
            // given
            SocialUserInfo info = buildUserInfo("social-123", "social@example.com");
            OAuthProvider provider = OAuthProvider.GOOGLE;

            when(userRepository.findBySocialId("social-123", provider))
                    .thenReturn(Optional.empty());

            UserSocialAccount acc1 = UserSocialAccount.builder().build();
            UserSocialAccount acc2 = UserSocialAccount.builder().build();
            when(socialAccountRepository.findBySocialEmail("social@example.com"))
                    .thenReturn(List.of(acc1, acc2));

            // when
            SocialUserFlowService.SocialAuthResult result =
                    socialUserFlowService.analyzeUser(info, provider);

            // then
            assertEquals(SocialAuthStatus.ERROR, result.getStatus());
            assertEquals(
                    "Найдено несколько социальных аккаунтов с этим email! Запрещено создавать аккаунты с одинаковыми email",
                    result.getMessage()
            );
            assertNull(result.getBaseUserEmail());
            assertEquals(info, result.getSocialUser());
            assertNull(result.getAuthCode()); // при ERROR код не выдаётся

            verify(userRepository, times(1)).findBySocialId("social-123", provider);
            verify(socialAccountRepository, times(1)).findBySocialEmail("social@example.com");
            verifyNoInteractions(oneTimeAuthCodeService);
        }

        @Test
        @DisplayName("Ровно один social-аккаунт с этим email -> NEW_SOCIAL_USER и выдача кода")
        void analyzeUser_SingleSocialAccountByEmail_ReturnsNewSocialUser() {
            // given
            SocialUserInfo info = buildUserInfo("social-123", "social@example.com");
            OAuthProvider provider = OAuthProvider.GOOGLE;

            when(userRepository.findBySocialId("social-123", provider))
                    .thenReturn(Optional.empty());

            User user = User.builder()
                    .email("base@example.com")
                    .build();
            UserSocialAccount account = UserSocialAccount.builder()
                    .socialEmail("social@example.com")
                    .user(user)
                    .build();

            when(socialAccountRepository.findBySocialEmail("social@example.com"))
                    .thenReturn(List.of(account));
            when(oneTimeAuthCodeService.issueCode(info))
                    .thenReturn("auth-code-2");

            // when
            SocialUserFlowService.SocialAuthResult result =
                    socialUserFlowService.analyzeUser(info, provider);

            // then
            assertEquals(SocialAuthStatus.NEW_SOCIAL_USER, result.getStatus());
            assertEquals(
                    "Найден пользователь с таким e-mail среди социальных аккаунтов. Предложите привязку соцсети.",
                    result.getMessage()
            );
            assertEquals("base@example.com", result.getBaseUserEmail());
            assertEquals(info, result.getSocialUser());
            assertEquals("auth-code-2", result.getAuthCode());

            verify(userRepository, times(1)).findBySocialId("social-123", provider);
            verify(socialAccountRepository, times(1)).findBySocialEmail("social@example.com");
            verify(oneTimeAuthCodeService, times(1)).issueCode(info);
        }

        @Test
        @DisplayName("Список social-аккаунтов null или пустой -> переход к поиску по основному email")
        void analyzeUser_NoSocialAccountsByEmail_GoesToEmailSearch() {
            // given
            SocialUserInfo info = buildUserInfo("social-123", "social@example.com");
            OAuthProvider provider = OAuthProvider.GOOGLE;

            when(userRepository.findBySocialId("social-123", provider))
                    .thenReturn(Optional.empty());
            when(socialAccountRepository.findBySocialEmail("social@example.com"))
                    .thenReturn(Collections.emptyList());

            User userByEmail = User.builder()
                    .email("user-main@example.com")
                    .build();
            when(userRepository.findByEmail("social@example.com"))
                    .thenReturn(Optional.of(userByEmail));
            when(oneTimeAuthCodeService.issueCode(info))
                    .thenReturn("auth-code-3");

            // when
            SocialUserFlowService.SocialAuthResult result =
                    socialUserFlowService.analyzeUser(info, provider);

            // then
            assertEquals(SocialAuthStatus.EMAIL_LINKED, result.getStatus());
            assertEquals(
                    "E-mail этой соцсети уже используется в другой учётке. Авторизуйтесь стандартным способом и привяжите соцсеть через профиль.",
                    result.getMessage()
            );
            assertEquals("user-main@example.com", result.getBaseUserEmail());
            assertEquals(info, result.getSocialUser());
            assertEquals("auth-code-3", result.getAuthCode());

            verify(userRepository, times(1)).findBySocialId("social-123", provider);
            verify(socialAccountRepository, times(1)).findBySocialEmail("social@example.com");
            verify(userRepository, times(1)).findByEmail("social@example.com");
            verify(oneTimeAuthCodeService, times(1)).issueCode(info);
        }
    }

    @Nested
    @DisplayName("analyzeUser: поиск по основному email и новый аккаунт")
    class AnalyzeUserByEmailAndNew {

        @Test
        @DisplayName("Найден пользователь по основному email -> EMAIL_LINKED и выдача кода")
        void analyzeUser_UserFoundByEmail_ReturnsEmailLinked() {
            // given
            SocialUserInfo info = buildUserInfo("social-xyz", "main@example.com");
            OAuthProvider provider = OAuthProvider.VK;

            when(userRepository.findBySocialId("social-xyz", provider))
                    .thenReturn(Optional.empty());
            when(socialAccountRepository.findBySocialEmail("main@example.com"))
                    .thenReturn(null); // эмулируем null, как в коде учтено

            User userByEmail = User.builder()
                    .email("main@example.com")
                    .build();
            when(userRepository.findByEmail("main@example.com"))
                    .thenReturn(Optional.of(userByEmail));
            when(oneTimeAuthCodeService.issueCode(info))
                    .thenReturn("auth-code-4");

            // when
            SocialUserFlowService.SocialAuthResult result =
                    socialUserFlowService.analyzeUser(info, provider);

            // then
            assertEquals(SocialAuthStatus.EMAIL_LINKED, result.getStatus());
            assertEquals(
                    "E-mail этой соцсети уже используется в другой учётке. Авторизуйтесь стандартным способом и привяжите соцсеть через профиль.",
                    result.getMessage()
            );
            assertEquals("main@example.com", result.getBaseUserEmail());
            assertEquals(info, result.getSocialUser());
            assertEquals("auth-code-4", result.getAuthCode());

            verify(userRepository, times(1)).findBySocialId("social-xyz", provider);
            verify(socialAccountRepository, times(1)).findBySocialEmail("main@example.com");
            verify(userRepository, times(1)).findByEmail("main@example.com");
            verify(oneTimeAuthCodeService, times(1)).issueCode(info);
        }

        @Test
        @DisplayName("Пользователь не найден ни по socialId, ни по email -> NEW_ACCOUNT и выдача кода")
        void analyzeUser_NewAccount_ReturnsNewAccount() {
            // given
            SocialUserInfo info = buildUserInfo("social-new", "new@example.com");
            OAuthProvider provider = OAuthProvider.GOOGLE;

            when(userRepository.findBySocialId("social-new", provider))
                    .thenReturn(Optional.empty());
            when(socialAccountRepository.findBySocialEmail("new@example.com"))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findByEmail("new@example.com"))
                    .thenReturn(Optional.empty());
            when(oneTimeAuthCodeService.issueCode(info))
                    .thenReturn("auth-code-5");

            // when
            SocialUserFlowService.SocialAuthResult result =
                    socialUserFlowService.analyzeUser(info, provider);

            // then
            assertEquals(SocialAuthStatus.NEW_ACCOUNT, result.getStatus());
            assertEquals(
                    "Пользователь не найден. Можно продолжить регистрацию через соцсеть.",
                    result.getMessage()
            );
            assertNull(result.getBaseUserEmail());
            assertEquals(info, result.getSocialUser());
            assertEquals("auth-code-5", result.getAuthCode());

            verify(userRepository, times(1)).findBySocialId("social-new", provider);
            verify(socialAccountRepository, times(1)).findBySocialEmail("new@example.com");
            verify(userRepository, times(1)).findByEmail("new@example.com");
            verify(oneTimeAuthCodeService, times(1)).issueCode(info);
        }
    }
}