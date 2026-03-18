package fr.mossaab.security.service;

import fr.mossaab.security.dto.SmsRuCallResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service("CALL")
@RequiredArgsConstructor
public class SmsRuCallGateway implements PhoneVerificationGateway {

    @Value("${sms.call.api-id}")
    private String apiId;

    private final RestTemplate restTemplate;

    @Override
    public String sendCode(String phone) {
        try {
            // 1) убираем «+», только цифры
            String digits = phone.replaceAll("\\D", "");
            // 2) правильный URL: /code/call
            String url = "https://sms.ru/code/call?api_id=%s&phone=%s&json=1"
                    .formatted(apiId, digits);

            // 3) отлавливаем 4xx/5xx сами, чтобы дать внятную ошибку
            restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
                @Override
                public void handleError(ClientHttpResponse response) throws IOException {
                    // не выбрасываем, а позволяем разобрать тело
                }
            });

            SmsRuCallResponse rsp = restTemplate.getForObject(new URI(url), SmsRuCallResponse.class);

            if (rsp == null) {
                throw new RuntimeException("SMS-ru вернул пустой ответ");
            }

            if (!"OK".equals(rsp.getStatus())) {
                throw new RuntimeException("SMS-ru ошибка: " + rsp.getStatusText());
            }

            return rsp.getCode(); // последние 4 цифры звонящего номера
        } catch (URISyntaxException e) {
            throw new RuntimeException("Некорректный URL для SMS-ru", e);
        } catch (RestClientException e) {
            throw new RuntimeException("Не удалось вызвать SMS-ru: " + e.getMessage(), e);
        }
    }
}
