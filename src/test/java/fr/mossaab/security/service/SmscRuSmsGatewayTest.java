package fr.mossaab.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests Для сервиса SmscRuSmsGateway")
class SmscRuSmsGatewayTest {

    @InjectMocks
    private SmscRuSmsGateway smscRuSmsGateway;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        smscRuSmsGateway = new SmscRuSmsGateway(restTemplate);
        ReflectionTestUtils.setField(smscRuSmsGateway, "login", "testLogin");
        ReflectionTestUtils.setField(smscRuSmsGateway, "password", "testPassword");
    }

    @Test
    @DisplayName("Успешная отправка кода подтверждения")
    void sendCode_Success() {
        when(restTemplate.getForObject(anyString(), eq(Object.class))).thenReturn(new Object());

        String phone = "+71234567890";
        String code = smscRuSmsGateway.sendCode(phone);

        assertTrue(Pattern.matches("\\d{4}", code), "Код подтверждения должен быть 4-значным числом");

        verify(restTemplate, times(1)).getForObject(anyString(), eq(Object.class));
    }

    @Test
    @DisplayName("Проверка корректности формируемого URL")
    void sendCode_UrlCheck() {
        String phone = "+71234567890";
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        when(restTemplate.getForObject(urlCaptor.capture(), eq(Object.class))).thenReturn(new Object());

        smscRuSmsGateway.sendCode(phone);

        String actualUrl = urlCaptor.getValue();

        assertTrue(actualUrl.contains("https://smsc.ru/sys/send.php"), "URL должен содержать базовый путь API");
        assertTrue(actualUrl.contains("login=testLogin"), "URL должен содержать корректный login");
        assertTrue(actualUrl.contains("psw=testPassword"), "URL должен содержать корректный password");
        assertTrue(actualUrl.contains("phones=" + phone), "URL должен содержать телефонный номер");
        assertTrue(actualUrl.contains("mes="), "URL должен содержать закодированное сообщение");
    }
}