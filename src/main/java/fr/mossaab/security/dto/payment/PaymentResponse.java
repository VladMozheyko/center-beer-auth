package fr.mossaab.security.dto.payment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {

    @Schema(description = "Сообщение об успешной покупке", example = "Покупка прошла успешно")
    private String message;

    @Schema(description = "Общее количество груш у пользователя после покупки", example = "5")
    private Integer pears;
}