package fr.mossaab.security.service;
import fr.mossaab.security.dto.payment.PurchaseRequest;
import fr.mossaab.security.entities.Payment;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.PaymentRepository;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    private static final Map<String, Integer> PRODUCT_PEAR_MAP = Map.of(
            "pear_id", 1
    );

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
