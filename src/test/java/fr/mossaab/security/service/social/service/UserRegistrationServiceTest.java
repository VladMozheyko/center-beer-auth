package fr.mossaab.security.service.social.service;


import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты для UserRegistrationService")
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    private SocialUserInfo buildInfo(String id, String email, String firstName, String lastName) {
        SocialUserInfo info = new SocialUserInfo();
        info.setId(id);
        info.setEmail(email);
        info.setFirstName(firstName);
        info.setLastName(lastName);
        return info;
    }

    // ---------- ТЕСТЫ ПУБЛИЧНОГО МЕТОДА registerNewUser ----------

    @Nested
    @DisplayName("registerNewUser: общая логика регистрации")
    class RegisterNewUserTests {

        @Test
        @DisplayName("Регистрация с реальным email — email не подменяется, роль USER, балансы по умолчанию")
        void registerNewUser_WithRealEmail() {
            // given
            SocialUserInfo info = buildInfo(
                    "ext-1",
                    "real@example.com",
                    "Иван",
                    "Иванов"
            );

            when(userRepository.existsByNickname(anyString())).thenReturn(false);
            // save возвращает тот же объект
            Mockito.doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                // эмулируем, что Hibernate заполнил id
                u.setId(1L);
                return null;
            }).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.GOOGLE, info);

            // then
            assertNotNull(user);
            assertEquals("real@example.com", user.getEmail());
            assertEquals(Role.USER, user.getRole());
            assertNotNull(user.getCreatedAt());
            assertEquals(0, user.getTemporarySecondsBalance());
            assertFalse(user.getPhoneVerified());
            assertNotNull(user.getSocialAccounts());
            assertTrue(user.getSocialAccounts().isEmpty());

            // createdAt "сейчас" протестируем хотя бы на то, что не в будущем
            assertTrue(user.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

            assertNotNull(user.getNickname());
            assertFalse(user.getNickname().isBlank());

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Регистрация без email — генерируется фейковый email")
        void registerNewUser_WithoutEmail_GeneratesFakeEmail() {
            // given
            SocialUserInfo info = buildInfo(
                    "ext-2",
                    null,
                    "Пётр",
                    "Петров"
            );

            when(userRepository.existsByNickname(anyString())).thenReturn(false);
            Mockito.doAnswer(invocation -> null).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.VK, info);

            // then
            assertTrue(user.getEmail().startsWith("vk_ext-2@no-email.com"));
            assertEquals(Role.USER, user.getRole());
            assertNotNull(user.getNickname());
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Регистрация без имени и фамилии, но с email — ник генерируется из email (транслит, замена спецсимволов)")
        void registerNewUser_NicknameFromEmail_WhenNoNames() {
            // given
            SocialUserInfo info = buildInfo(
                    "ext-3",
                    "user.name+tag@example.com",
                    null,
                    null
            );

            when(userRepository.existsByNickname(anyString())).thenReturn(false);
            Mockito.doAnswer(invocation -> null).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.YANDEX, info);

            // then
            assertEquals("user_name_tag", user.getNickname());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Регистрация без имени, фамилии и email — ник по умолчанию 'user' с проверкой уникальности")
        void registerNewUser_NoNamesNoEmail_DefaultNicknameUserWithUniqueness() {
            // given
            SocialUserInfo info = buildInfo("ext-4", null, null, null);

            // existsByNickname("user") -> true (занято), "user_1" -> false
            when(userRepository.existsByNickname("user")).thenReturn(true);
            when(userRepository.existsByNickname("user_1")).thenReturn(false);

            Mockito.doAnswer(invocation -> null).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.GOOGLE, info);

            // then
            assertEquals("user_1", user.getNickname());
            assertEquals("google_ext-4@no-email.com", user.getEmail());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Регистрация: множество socialAccounts инициализируется пустым HashSet")
        void registerNewUser_SocialAccountsInitializedAsEmptySet() {
            // given
            SocialUserInfo info = buildInfo("ext-5", "aa@bb.cc", "Иван", "Сидоров");
            when(userRepository.existsByNickname(anyString())).thenReturn(false);
            Mockito.doAnswer(invocation -> null).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.GOOGLE, info);

            // then
            assertNotNull(user.getSocialAccounts());
            assertTrue(user.getSocialAccounts().isEmpty());
            assertEquals(HashSet.class, user.getSocialAccounts().getClass());
        }
    }

    // ---------- НЕПРЯМОЕ ТЕСТИРОВАНИЕ PRIVATE-ЛОГИКИ НИКА ЧЕРЕЗ registerNewUser ----------

    @Nested
    @DisplayName("Генерация ника: уникальность и ограничения длины")
    class NicknameGenerationTests {

        @Test
        @DisplayName("Если базовый nickname уже существует — добавляется числовой суффикс")
        void nickname_SuffixAddedWhenExists() {
            // given
            SocialUserInfo info = buildInfo("ext-6", "nick@example.com", "Иван", "Иванов");

            when(userRepository.existsByNickname("ivan_ivanov")).thenReturn(true);
            when(userRepository.existsByNickname("ivan_ivanov_1")).thenReturn(true);
            when(userRepository.existsByNickname("ivan_ivanov_2")).thenReturn(false);

            Mockito.doAnswer(invocation -> null).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.GOOGLE, info);

            // then
            assertEquals("ivan_ivanov_2", user.getNickname());
        }

        @Test
        @DisplayName("Если при добавлении суффикса ник становится длиннее 50 — база обрезается")
        void nickname_LongBaseName_TruncatedWithSuffix() {
            // given
            String veryLongName = "ОченьДлинноеИмяПользователяКотороеСильноБольшеНормы";
            String veryLongLastName = "СверхДлиннаяФамилияКотораяТожеДлинная";

            SocialUserInfo info = buildInfo("ext-7", "long@example.com", veryLongName, veryLongLastName);

            // Чтобы проверить ветку, когда при добавлении суффикса превышаем 50 символов:
            // 1) Пусть существует ник без суффикса
            // 2) Существует с суффиксом 1 (и с ним длиннее 50)
            // 3) Для следующей попытки метод обрежет базу и сделает новый ник.
            when(userRepository.existsByNickname(anyString()))
                    .thenAnswer(invocation -> {
                        String nick = invocation.getArgument(0, String.class);
                        // первая проверка - базовый ник: пусть существует
                        if (!nick.contains("_1")) {
                            return true;
                        }
                        // вторая - с суффиксом _1: тоже пусть существует
                        if (nick.endsWith("_1")) {
                            return true;
                        }
                        // для любых других - пусть будет свободным
                        return false;
                    });

            Mockito.doAnswer(invocation -> null).when(userRepository).save(any(User.class));

            // when
            User user = userRegistrationService.registerNewUser(OAuthProvider.GOOGLE, info);

            // then
            String nickname = user.getNickname();
            assertNotNull(nickname);
            assertTrue(nickname.length() <= 50);
        }
    }

    // ---------- ТЕСТЫ PRIVATE-МЕТОДА transliterateCyrillic ЧЕРЕЗ REFLECTION (по желанию) ----------

    @Nested
    @DisplayName("transliterateCyrillic: проверка правил транслитерации (reflection)")
    class TransliterationTests {

        @Test
        @DisplayName("Корректная транслитерация базовых русских букв и фильтрация символов")
        void transliterateCyrillic_BasicCheck() throws Exception {
            // обращаемся к private-методу через reflection
            Method m = UserRegistrationService.class
                    .getDeclaredMethod("transliterateCyrillic", String.class);
            m.setAccessible(true);

            String src = "Привет, мир! Ёжик в тумане";
            String result = (String) m.invoke(userRegistrationService, src);

            // pривет -> privet, ёжик -> ezhik, " " -> "_" и т.д.
            assertEquals("privet_mir_ezhik_v_tumane", result);
        }

        @Test
        @DisplayName("transliterateCyrillic: null и пустая строка")
        void transliterateCyrillic_NullAndEmpty() throws Exception {
            Method m = UserRegistrationService.class
                    .getDeclaredMethod("transliterateCyrillic", String.class);
            m.setAccessible(true);

            assertEquals("", m.invoke(userRegistrationService, (Object) null));
            assertEquals("", m.invoke(userRegistrationService, ""));
        }
    }

    // ---------- ТЕСТЫ PRIVATE-МЕТОДА generateFakeEmail ЧЕРЕЗ REFLECTION (опционально) ----------

    @Nested
    @DisplayName("generateFakeEmail: формат фейкового email (reflection)")
    class GenerateFakeEmailTests {

        @Test
        @DisplayName("Формат: provider_lowercase_externalId@no-email.com")
        void generateFakeEmail_Format() throws Exception {
            Method m = UserRegistrationService.class
                    .getDeclaredMethod("generateFakeEmail", OAuthProvider.class, String.class);
            m.setAccessible(true);

            String email = (String) m.invoke(userRegistrationService, OAuthProvider.GOOGLE, "12345");
            assertEquals("google_12345@no-email.com", email);

            email = (String) m.invoke(userRegistrationService, OAuthProvider.VK, "xyz");
            assertEquals("vk_xyz@no-email.com", email);
        }
    }
}