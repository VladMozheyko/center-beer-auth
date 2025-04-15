package fr.mossaab.security.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import fr.mossaab.security.config.SignatureUtil;
import fr.mossaab.security.dto.payment.InvoiceStatusData;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;
import fr.mossaab.security.entities.Payment;
import fr.mossaab.security.entities.PurchaseMapping;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.PaymentRepository;
import fr.mossaab.security.repository.PurchaseMappingRepository;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PurchaseMappingRepository purchaseMappingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private BigDecimal fetchAmountFromRuStore(String purchaseToken) {
        try {
            String url = "https://public-api.rustore.ru/public/purchase/" + purchaseToken;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Public-Token", "ТВОЙ_API_ТОКЕН"); // 🔐 замени на свой

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = (Map<String, Object>) response.getBody().get("body");
            Map<String, Object> paymentInfo = (Map<String, Object>) body.get("payment_info");
            double amount = Double.parseDouble(paymentInfo.get("amount").toString());

            return BigDecimal.valueOf(amount);
        } catch (Exception e) {
            System.out.println("⚠️ Ошибка получения суммы из RuStore API: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private int convertAmountToPears(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(100)) == 0) return 1;
        if (amount.compareTo(BigDecimal.valueOf(1000)) == 0) return 10;
        if (amount.compareTo(BigDecimal.valueOf(5000)) == 0) return 50;
        if (amount.compareTo(BigDecimal.valueOf(10000)) == 0) return 100;
        return 0;
    }

    public int verifyAndHandlePurchase(VerifiedPurchaseRequest request) {
        if (request.getSignature() == null || request.getOrderId() == null) {
            return 0; // тестовая покупка
        }

        boolean validSignature = SignatureUtil.verifySignature(request);
        if (!validSignature) throw new SecurityException("Invalid signature from RuStore");

        if (paymentRepository.existsByTransactionId(request.getOrderId())) {
            return 0; // уже обработано
        }

        Long userId = Long.valueOf(request.getDeveloperPayload());
        int pears = calculatePears(request.getProductId(), request.getQuantity());

        Payment payment = Payment.builder()
                .userId(userId)
                .productId(request.getProductId())
                .transactionId(request.getOrderId())
                .amount(BigDecimal.valueOf(pears * 100))
                .confirmed(true)
                .build();

        paymentRepository.save(payment);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPears(user.getPears() + pears);
        userRepository.save(user);

        return user.getPears();
    }


    public int handleInvoice(InvoiceStatusData invoice) {
        String purchaseId = invoice.getPurchaseId();     // bdc9bfe8-78b8-4361-a017-7cc62fa37d6f
        String orderId = invoice.getOrderId();           // 1
        String productCode = invoice.getProductCode();    // pear_id / pear_pack_10 / ...

        // 1. Пытаемся взять userId из нашей БД, куда ранее записали (purchaseId -> userId)
        Long userId = purchaseMappingRepository.findByPurchaseId(purchaseId)
                .map(PurchaseMapping::getUserId)
                .orElse(null);

        // Если userId не нашли, значит /payment/mapping ещё не успели вызвать или что-то пошло не так
        if (userId == null) {
            System.out.println("❗ Не найден userId для purchaseId: " + purchaseId);
            return 0; // Прекращаем обработку, чтобы не дублировать платёж
        }

        // 2. Проверяем, вдруг мы уже обрабатывали такой orderId (RuStore присылает несколько статусов)
        if (paymentRepository.existsByTransactionId(orderId)) {
            System.out.println("🔎 Транзакция " + orderId + " уже есть. Пропускаем повторный статус: " + invoice.getStatusNew());
            return 0; // Дублирование
        }

        // 3. Узнаём quantity — либо из invoice, либо (если 0) через Public API RuStore по purchaseToken
        int quantityFromInvoice = invoice.getQuantity();
        if (quantityFromInvoice <= 0) {
            // Если вдруг RuStore не прислало quantity в колбэке
            quantityFromInvoice = fetchQuantityFromRuStore(invoice.getPurchaseToken());
        }

        // 4. Считаем, сколько «груш» положено за productCode (с учётом quantity)
        int pears = calculatePears(productCode, quantityFromInvoice);

        // 5. Проставим какую-нибудь условную сумму, например 1 груша = 100 копеек
        BigDecimal amount = BigDecimal.valueOf(pears * 100);

        // 6. Сохраняем запись о платеже в БД
        Payment payment = Payment.builder()
                .userId(userId)
                .productId(productCode)
                .transactionId(orderId)
                .amount(amount)
                .confirmed(true)
                .build();

        paymentRepository.save(payment);

        // 7. Начисляем «груши» пользователю
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPears(user.getPears() + pears);
        userRepository.save(user);

        System.out.println("✅ Начислено " + pears + " груш пользователю ID=" + userId);
        return user.getPears();
    }

    private int fetchQuantityFromRuStore(String purchaseToken) {
        try {
            // 1) URL из документации RuStore (примерный)
            String url = "https://public-api.rustore.ru/public/v1/payment-info?token=" + purchaseToken;

            // 2) Заголовки
            HttpHeaders headers = new HttpHeaders();
            // Например, если нужен Public-Token:
            // headers.set("Public-Token", "ВАШ_ПУБЛИК_ТОКЕН");
            // или если нужен Bearer-токен:
            // headers.set("Authorization", "Bearer ВАШ_ПУБЛИК_ТОКЕН");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            // 3) Выполняем запрос
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            // 4) Парсим JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            // Допустим, в "body" лежит объект с полем "quantity"
            int quantity = root.path("body").path("quantity").asInt(1); // fallback = 1

            System.out.println("🎯 Из RuStore API получили quantity=" + quantity);
            return quantity;

        } catch (Exception e) {
            System.out.println("⚠️ Ошибка при запросе к RuStore: " + e.getMessage());
            // Если не получилось — возвращаем 1 по умолчанию
            return 1;
        }
    }



    private int calculatePears(String productCode, int quantity) {
        return switch (productCode) {
            case "pear_id"       -> 1   * quantity;
            case "pear_pack_10"  -> 10  * quantity;
            case "pear_pack_50"  -> 50  * quantity;
            case "pear_pack_100" -> 100 * quantity;
            default              -> 0;
        };
    }

}
