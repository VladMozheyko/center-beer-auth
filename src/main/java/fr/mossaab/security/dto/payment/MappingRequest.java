package fr.mossaab.security.dto.payment;

public class MappingRequest {
    private String purchaseId;
    private Long userId;

    // геттеры и сеттеры
    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}