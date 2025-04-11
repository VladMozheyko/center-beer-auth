package fr.mossaab.security.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VerifiedPurchaseRequest {
    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("purchase_id")
    private String purchaseId;

    @JsonProperty("developer_payload")
    private String developerPayload;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("signature")
    private String signature;
}
