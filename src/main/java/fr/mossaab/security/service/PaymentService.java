package fr.mossaab.security.service;
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
            System.out.println("🧪 Получено тестовое уведомление. Подпись отсутствует — пропуск.");
            return 0;
        }

        boolean validSignature = SignatureUtil.verifySignature(request);
        if (!validSignature) {
            System.out.println("❌ Невалидная подпись.");
            throw new SecurityException("Invalid signature from RuStore");
        }

        if (paymentRepository.existsByTransactionId(request.getOrderId())) {
            System.out.println("⚠️ Транзакция уже обработана: " + request.getOrderId());
            return 0;
        }

        Long userId = Long.valueOf(request.getDeveloperPayload());
        int quantity = request.getQuantity() > 0 ? request.getQuantity() : 1;
        int pears = calculatePears(request.getProductId(), quantity);

        BigDecimal amount = BigDecimal.valueOf(pears * 100); // 👉 по 100 копеек за 1 грушу

        Payment payment = Payment.builder()
                .userId(userId)
                .productId(request.getProductId())
                .transactionId(request.getOrderId())
                .amount(amount)
                .confirmed(true)
                .build();

        paymentRepository.save(payment);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPears(user.getPears() + pears);
        userRepository.save(user);

        System.out.println("✅ Боевой платёж. Пользователю " + userId + " начислено " + pears + " груш");

        return user.getPears();
    }


    public int handleInvoice(InvoiceStatusData invoice) {
        String purchaseId = invoice.getPurchaseId();
        String orderId = invoice.getOrderId();
        String token = invoice.getPurchaseToken();

        Long userId = null;

        if (invoice.getDeveloperPayload() != null) {
            userId = Long.valueOf(invoice.getDeveloperPayload());
        }

        if (userId == null) {
            userId = purchaseMappingRepository.findByPurchaseId(purchaseId)
                    .map(PurchaseMapping::getUserId)
                    .orElse(null);
        }

        if (userId == null) {
            System.out.println("⚠️ Не удалось определить userId для покупки: " + purchaseId);
            return 0;
        }

        if (paymentRepository.existsByTransactionId(orderId)) {
            System.out.println("⚠️ Транзакция уже обработана: " + orderId);
            return 0;
        }

        // 👇 Получаем сумму из RuStore API
        BigDecimal amount = fetchAmountFromRuStore(token);
        int pears = convertAmountToPears(amount);

        Payment payment = Payment.builder()
                .userId(userId)
                .productId(invoice.getProductCode())
                .transactionId(orderId)
                .amount(amount)
                .confirmed(true)
                .build();

        paymentRepository.save(payment);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPears(user.getPears() + pears);
        userRepository.save(user);

        System.out.println("✅ Пользователю " + userId + " начислено " + pears + " груш");

        return pears;
    }


    private int calculatePears(String productCode, int quantity) {
        return switch (productCode) {
            case "pear_id" -> 1 * quantity; // 👈 Добавлена базовая покупка "Груша"
            case "pear_pack_10", "pear_id_10" -> 10 * quantity;
            case "pear_pack_50", "pear_id_50" -> 50 * quantity;
            case "pear_pack_100", "pear_id_100" -> 100 * quantity;
            default -> throw new IllegalArgumentException("Неизвестный товар: " + productCode);
        };
    }
}
