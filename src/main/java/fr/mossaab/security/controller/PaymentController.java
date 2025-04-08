package fr.mossaab.security.controller;
import fr.mossaab.security.dto.payment.PurchaseRequest;
import fr.mossaab.security.dto.payment.PaymentResponse;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;
import fr.mossaab.security.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Платежи", description = "Покупка внутриигровой валюты через RuStore")
public class PaymentController {

    private final PaymentService paymentService;
    @Operation(
            summary = "Подтверждение платежа от RuStore",
            description = """
        Этот метод вызывается **автоматически** RuStore в момент подтверждения покупки пользователем.

        🔒 **Безопасность**:
        - В теле запроса содержится объект VerifiedPurchaseRequest с подробностями покупки.
        - Каждое уведомление содержит поле `signature`, которое необходимо верифицировать с помощью **публичного ключа RuStore**.
        - Публичный ключ можно получить с URL: `https://ds.rustore.ru/keys/public_key.pem`.
        - Верификация осуществляется локально через алгоритм `SHA256withRSA`.

        ⚙️ **Принцип работы**:
        1. Сервер получает подписанный RuStore объект VerifiedPurchaseRequest.
        2. Выполняется верификация цифровой подписи (signature).
        3. Если подпись действительна — пользователю начисляются груши (pear).
        4. Повторные транзакции (с одним и тем же `transactionId`) не обрабатываются повторно.

        📌 **Требования**:
        - Этот endpoint **должен быть публичным**, доступным из интернета, т.к. вызывается сервером RuStore.
        - Защищается цифровой подписью, поэтому `JWT`, `auth` и т.п. здесь не обязательны.

        💰 **Начисление валюты**:
        - Курс по умолчанию: `100₽ = 1 груша`.
        - Начисляется на `userId`, указанный в теле VerifiedPurchaseRequest.

        📤 **Ответ**:
        Возвращает сообщение об успехе и обновлённый баланс пользователя в грушах.

        ❗ Пример тела запроса:
        ```json
        {
          "userId": 123,
          "productId": "pear_pack_1",
          "transactionId": "trx_987654321",
          "amount": 300.00,
          "signature": "MEUCIQDy7nN4..."
        }
        ```
    """
    )
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyAndProcessPayment(@RequestBody VerifiedPurchaseRequest request) {
        int updatedPears = paymentService.verifyAndHandlePurchase(request);
        return ResponseEntity.ok(new PaymentResponse("Покупка подтверждена", updatedPears));
    }
}