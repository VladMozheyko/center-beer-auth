package fr.mossaab.security.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.config.AesDecryptService;
import fr.mossaab.security.dto.payment.InvoiceStatusData;
import fr.mossaab.security.dto.payment.MappingRequest;
import fr.mossaab.security.dto.payment.RustoreCallbackRequest;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;
import fr.mossaab.security.entities.PurchaseMapping;
import fr.mossaab.security.repository.PurchaseMappingRepository;
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
    private final PurchaseMappingRepository repository;
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Привет");
    }

    @PostMapping("/mapping")
    public ResponseEntity<?> save(@RequestBody MappingRequest request) {
        String purchaseId = request.getPurchaseId();
        Long userId = request.getUserId();

        if (repository.findByPurchaseId(purchaseId).isEmpty()) {
            repository.save(PurchaseMapping.builder()
                    .purchaseId(purchaseId)
                    .userId(userId)
                    .build()
            );
        }
        return ResponseEntity.ok("✅ Mapping сохранён");
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

            JsonNode root = objectMapper.readTree(decryptedJson);
            String type = root.get("notification_type").asText();

            if ("PURCHASE".equals(type) || root.has("signature")) {
                VerifiedPurchaseRequest purchase = objectMapper.readValue(decryptedJson, VerifiedPurchaseRequest.class);
                int updatedPears = paymentService.verifyAndHandlePurchase(purchase);
                return ResponseEntity.ok("✅ Покупка обработана. Новый баланс: " + updatedPears);
            }

            return ResponseEntity.ok("🔔 Неизвестный тип уведомления: " + type);

        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки: " + e.getMessage());
            return ResponseEntity.badRequest().body("❌ Ошибка обработки: " + e.getMessage());
        }
    }

}
