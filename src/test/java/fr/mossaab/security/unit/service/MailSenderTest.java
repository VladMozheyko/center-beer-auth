package fr.mossaab.security.unit.service;

import fr.mossaab.security.service.MailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailSender mailSenderService;

    @BeforeEach
    void setUp() {
        // Устанавливаем значение username через рефлексию
        try {
            java.lang.reflect.Field field = MailSender.class.getDeclaredField("username");
            field.setAccessible(true);
            field.set(mailSenderService, "test@example.com");
        } catch (Exception e) {
            fail("Ошибка при установке значения username");
        }
    }

    @Test
    @DisplayName("Тест отправки письма с корректными параметрами")
    void testSendEmail_Success() {
        // Дано
        String to = "recipient@example.com";
        String subject = "Тест";
        String message = "Тестовое сообщение";

        // Когда
        mailSenderService.send(to, subject, message);

        // Тогда
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sentMessage = captor.getValue();
        assertEquals("test@example.com", sentMessage.getFrom());
        assertEquals(to, sentMessage.getTo()[0]);
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(message, sentMessage.getText());
    }
}