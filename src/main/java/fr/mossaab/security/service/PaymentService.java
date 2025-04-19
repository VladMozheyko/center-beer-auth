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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PurchaseMappingRepository purchaseMappingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

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
        String purchaseId = invoice.getPurchaseId();
        String orderId = invoice.getOrderId();
        String productCode = invoice.getProductCode();

        // ---------- 1) Парсим quantity из orderId (формат "|число|")
        int quantityFromOrderId = parseQuantityFromOrderId(orderId);

        // ---------- 2) Если не удалось вытащить (0 или <1), fallback на invoice.getQuantity()
        int quantityFinal = (quantityFromOrderId > 0)
                ? quantityFromOrderId
                : Math.max(1, invoice.getQuantity());

        // ---------- 3) Находим userId в своей БД (mapping)
        Long userId = purchaseMappingRepository.findByPurchaseId(purchaseId)
                .map(PurchaseMapping::getUserId)
                .orElse(null);
        if (userId == null) {
            System.out.println("❗ Не найден userId для purchaseId: " + purchaseId);
            return 0;
        }

        // ---------- 4) Проверяем, не обрабатывали ли orderId уже
        if (paymentRepository.existsByTransactionId(orderId)) {
            System.out.println("🔎 Транзакция " + orderId
                    + " уже есть. Пропускаем статус: " + invoice.getStatusNew());
            return 0;
        }

        // ---------- 5) Считаем, сколько груш положено за такой productCode (с учётом quantity)
        int pears = calculatePears(productCode, quantityFinal);

        // ---------- 6) Допустим, 1 груша = 100 копеек
        BigDecimal amount = BigDecimal.valueOf(pears * 100);

        // ---------- 7) Сохраняем оплату
        Payment payment = Payment.builder()
                .userId(userId)
                .productId(productCode)
                .transactionId(orderId)
                .amount(amount)
                .confirmed(true)
                .build();
        paymentRepository.save(payment);

        // ---------- 8) Начисляем «груши» пользователю
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPears(user.getPears() + pears);
        userRepository.save(user);

        System.out.println("✅ Начислено " + pears + " груш пользователю ID=" + userId);
        return user.getPears();
    }

    private static final Pattern QTY_PATTERN = Pattern.compile("quantity(\\d+)quantity");

    private int parseQuantityFromOrderId(String orderId) {
        if (orderId == null) return 0;

        // Ищем по регулярке "quantity<число>quantity"
        Matcher matcher = QTY_PATTERN.matcher(orderId);
        if (matcher.find()) {
            // matcher.group(1) – это то, что попало в скобки (\d+)
            String digits = matcher.group(1);
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ parseQuantityFromOrderId: не смогли преобразовать '"
                        + digits + "' в число. " + e.getMessage());
                return 0;
            }
        }

        // Если нет совпадений — возвращаем 0 (fallback)
        return 0;
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
