package fr.mossaab.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.config.AesDecryptService;
import fr.mossaab.security.dto.payment.RustoreCallbackRequest;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;
import fr.mossaab.security.dto.payment.PaymentResponse;
import fr.mossaab.security.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Привет");
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyCallback(@RequestBody RustoreCallbackRequest callbackRequest,
                                                 HttpServletRequest request) {
        try {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            System.out.println("📡 IP: " + ip);
            System.out.println("🧭 User-Agent: " + userAgent);
            System.out.println("📥 Encrypted Payload (Base64): " + callbackRequest.getPayload());

            // Расшифровка
            String decryptedJson = aesDecryptService.decrypt(callbackRequest.getPayload());
            System.out.println("🔓 Decrypted JSON: " + decryptedJson);

            // Парсинг в объект
            VerifiedPurchaseRequest purchase = objectMapper.readValue(decryptedJson, VerifiedPurchaseRequest.class);
            System.out.println("📦 Purchase: " + purchase);

            // Валидация подписи и логика
            int updatedPears = paymentService.verifyAndHandlePurchase(purchase);
            return ResponseEntity.ok("✅ Покупка обработана. Новый баланс: " + updatedPears);

        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Некорректный payload: " + e.getMessage());
            return ResponseEntity.badRequest().body("⚠️ Некорректный payload или формат");
        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("❌ Ошибка обработки: " + e.getMessage());
        }
    }


}