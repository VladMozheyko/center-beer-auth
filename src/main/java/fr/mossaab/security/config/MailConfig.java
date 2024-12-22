package fr.mossaab.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Конфигурационный класс MailConfig для настройки отправки почты.
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host; // Хост SMTP-сервера

    @Value("${spring.mail.username}")
    private String username; // Имя пользователя для аутентификации на SMTP-сервере

    @Value("${spring.mail.password}")
    private String password; // Пароль для аутентификации на SMTP-сервере

    @Value("${spring.mail.port}")
    private int port; // Порт SMTP-сервера

    @Value("${spring.mail.protocol}")
    private String protocol; // Протокол (например, SMTP)

    @Value("${mail.debug}")
    private String debug; // Флаг отладки для почтовых сообщений

    @Bean
    public JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Настройка свойств JavaMailSenderImpl
        mailSender.setHost(host); // Установка хоста SMTP-сервера
        mailSender.setPort(port); // Установка порта SMTP-сервера
        mailSender.setUsername(username); // Установка имени пользователя для аутентификации
        mailSender.setPassword(password); // Установка пароля для аутентификации

        // Настройка дополнительных свойств JavaMail
        Properties properties = mailSender.getJavaMailProperties();
        properties.setProperty("mail.transport.protocol", protocol); // Установка протокола
        properties.setProperty("mail.debug", debug); // Установка флага отладки

        return mailSender;
    }
}
