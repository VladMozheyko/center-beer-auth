package fr.mossaab.security.service;

import fr.mossaab.security.enums.RegistrationMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests для PhoneVerificationService")
class PhoneVerificationServiceTest {

    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private SmscRuSmsGateway smsGateway;

    @Mock
    private SmsRuCallGateway callGateway;

    private Map<String, PhoneVerificationGateway> gateways;

    @BeforeEach
    void setUp() {
        gateways = new HashMap<>();
        gateways.put("SMS", smsGateway);
        gateways.put("CALL", callGateway);
        ReflectionTestUtils.setField(phoneVerificationService, "gateways", gateways);
    }

    @Test
    @DisplayName("Отправка кода через SMS-шлюз")
    void sendCode_UsingSmsGateway_Success() {
        when(smsGateway.sendCode(anyString())).thenReturn("code123");

        String code = phoneVerificationService.sendCode("1234567890", RegistrationMethod.SMS);

        assertEquals("code123", code);
        verify(smsGateway, times(1)).sendCode("1234567890");
        verify(callGateway, never()).sendCode(anyString());
    }

    @Test
    @DisplayName("Отправка кода через CALL-шлюз")
    void sendCode_UsingCallGateway_Success() {
        when(callGateway.sendCode(anyString())).thenReturn("code789");

        String code = phoneVerificationService.sendCode("0987654321", RegistrationMethod.CALL);

        assertEquals("code789", code);
        verify(callGateway, times(1)).sendCode("0987654321");
        verify(smsGateway, never()).sendCode(anyString());
    }
}