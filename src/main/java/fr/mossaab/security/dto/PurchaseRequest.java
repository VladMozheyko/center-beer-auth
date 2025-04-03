package fr.mossaab.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseRequest {

    @Schema(description = "ID пользователя, совершающего покупку", example = "42")
    private Long userId;

    @Schema(description = "ID продукта из RuStore Console", example = "pear_id")
    private String productId;

    @Schema(description = "Уникальный идентификатор транзакции от RuStore", example = "trx_123456789")
    private String transactionId;

    @Schema(description = "Сумма покупки в рублях", example = "100.00")
    private BigDecimal amount;
}
