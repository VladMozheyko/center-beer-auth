package fr.mossaab.security.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.config.AesDecryptService;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;
import fr.mossaab.security.dto.payment.PaymentResponse;
import fr.mossaab.security.service.PaymentService;
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
    private final AesDecryptService aesDecryptService;
    private final ObjectMapper objectMapper;

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyAndProcessPayment(@RequestBody String encryptedRequestBase64) {
        try {
            System.out.println("📥 [INPUT] Encrypted Base64: " + encryptedRequestBase64);

            String decryptedJson = aesDecryptService.decrypt(encryptedRequestBase64);
            System.out.println("🔓 [DECRYPTED JSON] " + decryptedJson);

            VerifiedPurchaseRequest request = objectMapper.readValue(decryptedJson, VerifiedPurchaseRequest.class);
            System.out.println("📦 [Parsed Request] " + request);

            int updatedPears = paymentService.verifyAndHandlePurchase(request);
            return ResponseEntity.ok(new PaymentResponse("Покупка подтверждена", updatedPears));

        } catch (Exception e) {
            e.printStackTrace(); // ⛔ покажем stacktrace в лог
            return ResponseEntity.badRequest().body(new PaymentResponse("Ошибка обработки: " + e.getMessage(), -1));
        }
    }
}