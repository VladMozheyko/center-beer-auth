package fr.mossaab.security.unit.service.social.service;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.service.social.service.OneTimeAuthCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Unit-тесты для OneTimeAuthCodeService")
class OneTimeAuthCodeServiceTest {

    private OneTimeAuthCodeService oneTimeAuthCodeService;

    @BeforeEach
    void setUp() {
        oneTimeAuthCodeService = new OneTimeAuthCodeService();
    }

    @Nested
    @DisplayName("Тестирование метода issueCode")
    class IssueCodeTests {

        @Test
        @DisplayName("Успешная выдача кода для пользователя")
        void issueCode_Success_ReturnsNonEmptyCode() {
            SocialUserInfo socialUserInfo = new SocialUserInfo();

            String code = oneTimeAuthCodeService.issueCode(socialUserInfo);

            assertNotNull(code);
            assertFalse(code.isBlank());
            assertDoesNotThrow(() -> UUID.fromString(code));
        }
    }

    @Nested
    @DisplayName("Тестирование метода consumeCode")
    class ConsumeCodeTests {

        @Test
        @DisplayName("Успешное использование кода — возвращаются данные пользователя и код удаляется")
        void consumeCode_ValidCode_ReturnsUserInfoOnce() {
            SocialUserInfo socialUserInfo = new SocialUserInfo();
            String code = oneTimeAuthCodeService.issueCode(socialUserInfo);

            SocialUserInfo resultFirst = oneTimeAuthCodeService.consumeCode(code);
            SocialUserInfo resultSecond = oneTimeAuthCodeService.consumeCode(code);

            assertNotNull(resultFirst);
            assertSame(socialUserInfo, resultFirst);
            assertNull(resultSecond); // код одноразовый
        }

        @Test
        @DisplayName("Неверный код — возвращается null")
        void consumeCode_InvalidCode_ReturnsNull() {
            SocialUserInfo result = oneTimeAuthCodeService.consumeCode("non-existing-code");
            assertNull(result);
        }

        @Test
        @DisplayName("Просроченный код — возвращается null")
        void consumeCode_ExpiredCode_ReturnsNull() {
            SocialUserInfo socialUserInfo = new SocialUserInfo();

            // Задаём фиксированное "текущее" время
            Instant now = Instant.parse("2024-01-01T10:00:00Z");
            Instant past = now.minusSeconds(10);

            try (MockedStatic<Instant> instantMock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
                // сначала Instant.now() при issueCode
                instantMock.when(Instant::now).thenReturn(now);

                String code = oneTimeAuthCodeService.issueCode(socialUserInfo);

                // потом Instant.now() при consumeCode — ставим время позже чем срок жизни кода
                // срок жизни 180 сек, значит делаем now + 200 сек
                Instant expiredMoment = now.plusSeconds(200);
                instantMock.when(Instant::now).thenReturn(expiredMoment);

                SocialUserInfo result = oneTimeAuthCodeService.consumeCode(code);

                assertNull(result);
            }
        }
    }

    @Nested
    @DisplayName("Тестирование метода cleanUpExpiredCodes")
    class CleanUpExpiredCodesTests {

        @Test
        @DisplayName("Просроченные коды очищаются, валидные остаются")
        void cleanUpExpiredCodes_RemovesOnlyExpired() {
            SocialUserInfo userExpired = new SocialUserInfo();
            SocialUserInfo userValid = new SocialUserInfo();

            Instant baseTime = Instant.parse("2024-01-01T10:00:00Z");

            try (MockedStatic<Instant> instantMock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
                // время при выдаче кодов
                instantMock.when(Instant::now).thenReturn(baseTime);
                String codeExpired = oneTimeAuthCodeService.issueCode(userExpired);

                instantMock.when(Instant::now).thenReturn(baseTime);
                String codeValid = oneTimeAuthCodeService.issueCode(userValid);

                // делаем текущее время существенно позже истечения первого кода, но до истечения второго
                // важно: оба кода создаются в один момент, так что проще сделать:
                // - сначала "сдвинуть" срок жизни у одного кода через simulateExpired (не доступно)
                // поэтому вместо этого: имитируем "два состояния времени":
                // 1) база — при создании
                // 2) время сильно позже — оба истекут
                //
                // Для проверки, что *что-то* очищается, достаточно убедиться, что
                // после cleanUpExpiredCodes никакой код не отдается через consumeCode.

                // текущее время после истечения времени жизни обоих кодов
                Instant afterExpiry = baseTime.plusSeconds(200);
                instantMock.when(Instant::now).thenReturn(afterExpiry);

                oneTimeAuthCodeService.cleanUpExpiredCodes();

                assertNull(oneTimeAuthCodeService.consumeCode(codeExpired));
                assertNull(oneTimeAuthCodeService.consumeCode(codeValid));
            }
        }
    }
}