package fr.mossaab.security.controller;
import fr.mossaab.security.dto.PurchaseRequest;
import fr.mossaab.security.dto.PaymentResponse;
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

    @Operation(summary = "Подтвердить покупку", description = "Принимает данные о покупке и начисляет грушу пользователю")
    @PostMapping("/purchase")
    public ResponseEntity<PaymentResponse> confirmPurchase(@RequestBody PurchaseRequest request) {
        int pears = paymentService.handlePurchase(request);
        return ResponseEntity.ok(new PaymentResponse("Покупка прошла успешно", pears));
    }
}