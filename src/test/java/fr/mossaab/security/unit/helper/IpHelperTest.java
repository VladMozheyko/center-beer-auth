package fr.mossaab.security.unit.helper;

import fr.mossaab.security.helper.IpHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpHelperTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private IpHelper ipHelper;

    @BeforeEach
    void setUp() {
        ipHelper = new IpHelper();
    }

    @Test
    @DisplayName("Проверка извлечения IP из заголовка X-Real-IP")
    void testGetClientIp_XRealIp_Success() {
        // Given
        String expectedIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn(expectedIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
        verify(request, times(1)).getHeader("X-Real-IP");
    }

    @Test
    @DisplayName("Проверка извлечения IP из заголовка X-Real-IP с пробелами")
    void testGetClientIp_XRealIp_WithSpaces() {
        // Given
        String expectedIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn("  " + expectedIp + "  ");

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }

    @Test
    @DisplayName("Проверка извлечения IP из заголовка X-Real-IP когда заголовок пустой")
    void testGetClientIp_XRealIp_Empty() {
        // Given - пустая строка должна быть проигнорирована (isBlank), перейдем к X-Forwarded-For
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals("192.168.1.100", result);
    }

    @Test
    @DisplayName("Проверка извлечения внешнего IP из заголовка X-Forwarded-For")
    void testGetClientIp_XForwardedFor_ExternalIp() {
        // Given
        String externalIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1, " + externalIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(externalIp, result);
    }

    @Test
    @DisplayName("Проверка извлечения первого внешнего IP из X-Forwarded-For")
    void testGetClientIp_XForwardedFor_FirstExternalIp() {
        // Given
        String firstExternalIp = "203.0.113.5";
        String secondExternalIp = "198.51.100.23";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(firstExternalIp + ", " + secondExternalIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(firstExternalIp, result);
    }

    @Test
    @DisplayName("Проверка извлечения первого IP когда все внутренние в X-Forwarded-For")
    void testGetClientIp_XForwardedFor_AllInternalIps() {
        // Given
        String firstIp = "192.168.1.1";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(firstIp + ", 10.0.0.1, 127.0.0.1");

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(firstIp, result);
    }

    @Test
    @DisplayName("Проверка извлечения IP из getRemoteAddr когда нет заголовков")
    void testGetClientIp_RemoteAddr() {
        // Given
        String expectedIp = "192.168.1.100";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(expectedIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }

    @Test
    @DisplayName("Проверка определения внутреннего IP для loopback 127.0.0.1")
    void testIsInternalIp_Loopback127() {
        assertTrue(ipHelper.isInternalIp("127.0.0.1"));
        assertTrue(ipHelper.isInternalIp("127.0.0.5"));
        assertTrue(ipHelper.isInternalIp("127.255.255.255"));
    }

    @Test
    @DisplayName("Проверка определения внутреннего IP для приватных диапазонов")
    void testIsInternalIp_PrivateRanges() {
        // 10.x.x.x
        assertTrue(ipHelper.isInternalIp("10.0.0.1"));
        assertTrue(ipHelper.isInternalIp("10.255.255.255"));

        // 192.168.x.x
        assertTrue(ipHelper.isInternalIp("192.168.0.1"));
        assertTrue(ipHelper.isInternalIp("192.168.255.255"));

        // 172.16.x.x - 172.31.x.x
        assertTrue(ipHelper.isInternalIp("172.16.0.1"));
        assertTrue(ipHelper.isInternalIp("172.31.255.255"));

        // 169.254.x.x (link-local)
        assertTrue(ipHelper.isInternalIp("169.254.0.1"));
        assertTrue(ipHelper.isInternalIp("169.254.255.255"));
    }

    @Test
    @DisplayName("Проверка определения внешнего IP")
    void testIsInternalIp_ExternalIps() {
        assertFalse(ipHelper.isInternalIp("203.0.113.5"));
        assertFalse(ipHelper.isInternalIp("198.51.100.23"));
        assertFalse(ipHelper.isInternalIp("8.8.8.8"));
        assertFalse(ipHelper.isInternalIp("1.1.1.1"));
    }

    @Test
    @DisplayName("Проверка определения IPv6 loopback")
    void testIsInternalIp_Ipv6Loopback() {
        assertTrue(ipHelper.isInternalIp("::1"));
        // 0:0:0:0:0:0:0:1 тоже loopback
    }

    @Test
    @DisplayName("Проверка определения IPv6 приватных диапазонов")
    void testIsInternalIp_Ipv6Private() {
        assertTrue(ipHelper.isInternalIp("fd00::1"));
        assertTrue(ipHelper.isInternalIp("fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    @Test
    @DisplayName("Проверка обработки null IP")
    void testIsInternalIp_Null() {
        assertTrue(ipHelper.isInternalIp(null));
    }

    @Test
    @DisplayName("Проверка обработки пустого IP")
    void testIsInternalIp_Empty() {
        assertTrue(ipHelper.isInternalIp(""));
    }

    @Test
    @DisplayName("Проверка извлечения IP из X-Forwarded-For с пустыми значениями")
    void testGetClientIp_XForwardedFor_WithEmptyValues() {
        // Given
        String expectedIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" ,  , " + expectedIp + " ,  ");

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }

    @Test
    @DisplayName("Проверка приоритета заголовков: X-Real-IP > X-Forwarded-For > getRemoteAddr")
    void testGetClientIp_Priority_XRealIpWins() {
        // Given
        String expectedIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn(expectedIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }

    @Test
    @DisplayName("Проверка приоритета заголовков: X-Forwarded-For > getRemoteAddr")
    void testGetClientIp_Priority_XForwardedForWins() {
        // Given
        String expectedIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(expectedIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }

    @Test
    @DisplayName("Проверка обработки IPv4-mapped IPv6")
    void testIsInternalIp_Ipv4MappedIpv6() {
        // ::ffff:192.168.1.1 - это IPv4-адрес в IPv6 формате
        assertTrue(ipHelper.isInternalIp("::ffff:192.168.1.1"));
        assertTrue(ipHelper.isInternalIp("::ffff:127.0.0.1"));
        assertFalse(ipHelper.isInternalIp("::ffff:203.0.113.5"));
    }

    @Test
    @DisplayName("Проверка извлечения IP из X-Forwarded-For с одним значением")
    void testGetClientIp_XForwardedFor_SingleValue() {
        // Given
        String expectedIp = "203.0.113.5";
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(expectedIp);

        // When
        String result = ipHelper.getClientIp(request);

        // Then
        assertEquals(expectedIp, result);
    }
}
