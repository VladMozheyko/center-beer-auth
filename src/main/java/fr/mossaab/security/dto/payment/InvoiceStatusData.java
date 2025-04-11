package fr.mossaab.security.dto.payment;

import lombok.Data;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceStatusData {

    @JsonProperty("change_status_time")
    private String changeStatusTime;

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("status_new")
    private String statusNew;

    @JsonProperty("status_old")
    private String statusOld;

    @JsonProperty("purchase_token")
    private String purchaseToken;

    @JsonProperty("invoice_id")
    private String invoiceId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("purchase_id")
    private String purchaseId;

    @JsonProperty("developer_payload")
    private String developerPayload;

    @JsonProperty("quantity")
    private int quantity = 1;

    // 👇 Добавляем новое поле
    @JsonProperty("amount")
    private BigDecimal amount;
}
