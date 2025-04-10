package fr.mossaab.security.dto.advertisement;

import fr.mossaab.security.dto.payment.WithdrawalStatus;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.WithdrawalRequest;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalRequestDto {
    private Long id;
    private String username;
    private String paymentDetails;
    private Integer amount;
    private WithdrawalStatus status;
    private LocalDateTime createdAt;
    private String rejectionReason;

    public static WithdrawalRequestDto fromEntity(WithdrawalRequest request) {
        return WithdrawalRequestDto.builder()
                .id(request.getId())
                .username(request.getUser().getUsername()) // предполагаем, что User имеет getUsername()
                .paymentDetails(request.getPaymentDetails())
                .amount(request.getAmount())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .rejectionReason(request.getRejectionReason())
                .build();
    }
}