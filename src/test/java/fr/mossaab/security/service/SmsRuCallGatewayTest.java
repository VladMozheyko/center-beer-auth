package fr.mossaab.security.service;

import fr.mossaab.security.dto.SmsRuCallResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsRuCallGatewayTest {

    @InjectMocks
    private SmsRuCallGateway smsRuCallGateway;

    @Mock
    private RestTemplate restTemplate;

    @Value("${sms.call.api-id}")
    private String apiId = "test_api_id";

    @BeforeEach
    void setUp() {
        smsRuCallGateway = new SmsRuCallGateway(restTemplate);
    }

    @Test
    @DisplayName("Успешная отправка кода и получение ответа")
    void sendCode_Success() {
        String phone = "+1234567890";
        SmsRuCallResponse mockResponse = new SmsRuCallResponse();
        mockResponse.setStatus("OK");
        mockResponse.setStatusText("Запрос успешен");
        mockResponse.setCode("1234");

        when(restTemplate.getForObject(any(URI.class), eq(SmsRuCallResponse.class)))
                .thenReturn(mockResponse);

        String code = smsRuCallGateway.sendCode(phone);

        assertEquals("1234", code);
    }

    @Test
    @DisplayName("Ошибка: SMS-ru вернул пустой ответ")
    void sendCode_NullResponse_ThrowsException() {
        String phone = "+1234567890";

        when(restTemplate.getForObject(any(URI.class), eq(SmsRuCallResponse.class)))
                .thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> smsRuCallGateway.sendCode(phone));

        assertTrue(exception.getMessage().contains("SMS-ru вернул пустой ответ"));
    }

    @Test
    @DisplayName("Ошибка: SMS-ru вернул ошибочный статус")
    void sendCode_ErrorInResponseStatus_ThrowsException() {
        String phone = "+1234567890";
        SmsRuCallResponse mockResponse = new SmsRuCallResponse();
        mockResponse.setStatus("ERROR");
        mockResponse.setStatusText("Некорректный запрос");

        when(restTemplate.getForObject(any(URI.class), eq(SmsRuCallResponse.class)))
                .thenReturn(mockResponse);

        Exception exception = assertThrows(RuntimeException.class, () -> smsRuCallGateway.sendCode(phone));

        assertTrue(exception.getMessage().contains("SMS-ru ошибка: Некорректный запрос"));
    }


    @Test
    @DisplayName("Ошибка: Некорректный URL для SMS-ru")
    void sendCode_InvalidURL_ThrowsException() {
        // Испорченный API ID, который приведёт к построению некорректного URL
        smsRuCallGateway = new SmsRuCallGateway(restTemplate);
        ReflectionTestUtils.setField(smsRuCallGateway, "apiId", "<неправильный>");

        // Мокируем телефон так, чтобы итоговый URL был гарантированно неправильным
        String invalidPhone = "???123";

        Exception exception = assertThrows(RuntimeException.class, () -> smsRuCallGateway.sendCode(invalidPhone));

        assertTrue(exception.getCause() instanceof URISyntaxException, "Причина - не URISyntaxException");
        assertTrue(exception.getMessage().contains("Некорректный URL для SMS-ru"));
    }

    @Test
    @DisplayName("Ошибка: Не удалось вызвать SMS-ru")
    void sendCode_RestClientException_ThrowsException() {
        String phone = "+1234567890";

        when(restTemplate.getForObject(any(URI.class), eq(SmsRuCallResponse.class)))
                .thenThrow(new RestClientException("Connection error"));

        Exception exception = assertThrows(RuntimeException.class, () -> smsRuCallGateway.sendCode(phone));

        assertTrue(exception.getMessage().contains("Не удалось вызвать SMS-ru: Connection error"));
    }
}