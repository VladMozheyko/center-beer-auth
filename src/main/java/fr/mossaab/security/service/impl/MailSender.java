package fr.mossaab.security.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки электронной почты.
 */
@Service
public class MailSender {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String username;

    /**
     * Конструктор класса MailSender.
     *
     * @param mailSender Объект JavaMailSender для отправки писем
     */
    public MailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Отправляет электронное письмо.
     *
     * @param emailTo Получатель письма
     * @param subject Тема письма
     * @param message Текст письма
     */
    public void send(String emailTo, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setFrom(username);
        mailMessage.setTo(emailTo);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }
}
