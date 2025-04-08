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
        // 1. Проверка подписи
        boolean validSignature = SignatureUtil.verifySignature(request);
        if (!validSignature) {
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

    public int handlePurchase(PurchaseRequest request) {
        if (paymentRepository.existsByTransactionId(request.getTransactionId())) {
            throw new IllegalArgumentException("Transaction already processed");
        }

        // Курс: 10₽ = 1 груша
        int rublesPerPear = 100;
        int pearsToAdd = request.getAmount().intValue() / rublesPerPear;

        if (pearsToAdd <= 0) {
            throw new IllegalArgumentException("Слишком маленькая сумма. Минимум: " + rublesPerPear + "₽");
        }

        // Сохраняем транзакцию
        Payment payment = new Payment();
        payment.setUserId(request.getUserId());
        payment.setProductId(request.getProductId());
        payment.setTransactionId(request.getTransactionId());
        payment.setAmount(request.getAmount());
        payment.setConfirmed(true);
        paymentRepository.save(payment);

        // Обновляем баланс пользователя
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPears(user.getPears() + pearsToAdd);
        userRepository.save(user);

        return user.getPears();
    }

}
