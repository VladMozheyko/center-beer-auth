package fr.mossaab.security.dto.payment;

import lombok.Data;

@Data
public class InvoiceStatusData {
    private String changeStatusTime;
    private String productCode;
    private String statusNew;
    private String statusOld;
    private String purchaseToken;
    private String invoiceId;
    private String orderId;
    private String purchaseId;
    private String developerPayload;
}
