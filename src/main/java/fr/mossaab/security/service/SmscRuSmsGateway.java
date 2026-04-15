package fr.mossaab.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service("SMS")
@RequiredArgsConstructor
public class SmscRuSmsGateway implements PhoneVerificationGateway {

    @Value("${sms.smsc.login}")    private String login;
    @Value("${sms.smsc.password}") private String password;
    private final RestTemplate rest;

    @Override
    public String sendCode(String phone) {
        log.info("[SEND CODE] - Sending code {}", phone);
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
        String txt = URLEncoder.encode("Код подтверждения: " + code, StandardCharsets.UTF_8);
        String url = "https://smsc.ru/sys/send.php?login=%s&psw=%s&phones=%s&mes=%s&fmt=3"
                .formatted(login, password, phone, txt);
        rest.getForObject(url, Object.class); // запрос ушёл, ошибок нет
        log.info("[SEND CODE] - Sending code {} successful", code);
        return code;                          // вернём, чтобы сохранить у пользователя
    }
}
