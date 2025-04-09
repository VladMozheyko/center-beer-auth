package fr.mossaab.security.service;
import fr.mossaab.security.config.SignatureUtil;
import fr.mossaab.security.dto.payment.PurchaseRequest;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;
import fr.mossaab.security.entities.Payment;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.PaymentRepository;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public int verifyAndHandlePurchase(VerifiedPurchaseRequest request) {

        // 🔐 0. Проверка, тестовое ли уведомление
        if (request.getSignature() == null || request.getOrderId() == null) {
            System.out.println("🧪 Получено тестовое уведомление от RuStore. Подпись отсутствует — пропускаем обработку.");
            return 0;
        }

        // 1. Проверка подписи
        boolean validSignature = SignatureUtil.verifySignature(request);
        if (!validSignature) {
            System.out.println("❌ Невалидная подпись RuStore");
            throw new SecurityException("Invalid signature from RuStore");
        }

        // 2. Проверка на повтор
        if (paymentRepository.existsByTransactionId(request.getOrderId())) {
            throw new IllegalStateException("Transaction already processed");
        }

        Long userId = Long.valueOf(request.getDeveloperPayload());
        int pears = calculatePears(request.getProductId(), request.getQuantity());

        // 3. Сохраняем транзакцию
        Payment payment = Payment.builder()
                .userId(userId)
                .productId(request.getProductId())
                .transactionId(request.getOrderId())
                .amount(BigDecimal.valueOf(pears * 100)) // по курсу
                .confirmed(true)
                .build();

        paymentRepository.save(payment);

        // 4. Обновление баланса
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPears(user.getPears() + pears);
        userRepository.save(user);

        return user.getPears();
    }

    private int calculatePears(String productId, int quantity) {
        return switch (productId) {
            case "pear_pack_10" -> 10 * quantity;
            case "pear_pack_50" -> 50 * quantity;
            case "pear_pack_100" -> 100 * quantity;
            default -> throw new IllegalArgumentException("Unknown productId: " + productId);
        };
    }


}
