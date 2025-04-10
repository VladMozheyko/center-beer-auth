package fr.mossaab.security.service;

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

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PurchaseMappingRepository purchaseMappingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
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
            throw new IllegalStateException("Transaction already processed");
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

        System.out.println("✅ Боевой платёж. Пользователю " + userId + " начислено " + pears + " груш");

        return user.getPears();
    }

    public int handleInvoice(InvoiceStatusData invoice) {
        String purchaseId = invoice.getPurchaseId();
        String orderId = invoice.getOrderId();

        Long userId = null;

        // 1. Получаем userId из developerPayload, если есть
        if (invoice.getDeveloperPayload() != null) {
            userId = Long.valueOf(invoice.getDeveloperPayload());
        }

        // 2. Если developerPayload нет — пробуем взять из маппинга
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

        int pears = calculatePears(invoice.getProductCode(), 1);

        Payment payment = Payment.builder()
                .userId(userId)
                .productId(invoice.getProductCode())
                .transactionId(orderId)
                .amount(BigDecimal.valueOf(pears * 100))
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
            case "pear_pack_10", "pear_id_10" -> 10 * quantity;
            case "pear_pack_50", "pear_id_50" -> 50 * quantity;
            case "pear_pack_100", "pear_id_100" -> 100 * quantity;
            default -> throw new IllegalArgumentException("Неизвестный товар: " + productCode);
        };
    }
}
