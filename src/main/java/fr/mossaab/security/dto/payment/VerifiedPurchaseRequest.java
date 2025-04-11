package fr.mossaab.security.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VerifiedPurchaseRequest {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("package_name")
    private String packageName;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("purchase_time")
    private long purchaseTime;

    @JsonProperty("purchase_token")
    private String purchaseToken;

    @JsonProperty("purchase_state")
    private int purchaseState;

    @JsonProperty("purchase_type")
    private int purchaseType;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("developer_payload")
    private String developerPayload;

    @JsonProperty("signature")
    private String signature;
}
