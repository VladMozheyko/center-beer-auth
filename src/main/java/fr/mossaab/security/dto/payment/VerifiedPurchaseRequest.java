package fr.mossaab.security.dto.payment;

import lombok.Data;

@Data
public class VerifiedPurchaseRequest {
    private String orderId;
    private String packageName;
    private String productId;
    private long purchaseTime;
    private String purchaseToken;
    private int purchaseState;
    private int purchaseType;
    private int quantity;
    private String developerPayload; // userId как строка
    private String signature;
}