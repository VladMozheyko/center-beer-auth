package fr.mossaab.security.controller;

import com.sun.management.OperatingSystemMXBean;
import fr.mossaab.security.dto.advertisement.WithdrawalRequestDto;
import fr.mossaab.security.dto.payment.WithdrawalStatus;
import fr.mossaab.security.dto.user.GetAllUsersResponse;
import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.WithdrawalRequest;
import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.repository.AdvertisementRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.WithdrawalRequestRepository;
import fr.mossaab.security.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Администратор", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью администратор")
@RestController
@RequestMapping("/admin")
@SecurityRequirements()
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Operation(summary = "Список заявок на вывод с фильтрацией по статусу")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/withdrawals")
    public ResponseEntity<List<WithdrawalRequestDto>> getWithdrawalsByStatus(
            @RequestParam(required = false) WithdrawalStatus status
    ) {
        List<WithdrawalRequest> all = withdrawalRequestRepository.findAll();

        if (status != null) {
            all = all.stream()
                    .filter(request -> request.getStatus() == status)
                    .collect(Collectors.toList());
        }

        List<WithdrawalRequestDto> dtos = all.stream()
                .map(WithdrawalRequestDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Подтвердить заявку на вывод")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/withdrawals/approve")
    public ResponseEntity<String> approveWithdrawal(@RequestParam Long requestId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            return ResponseEntity.badRequest().body("Заявка уже обработана");
        }

        User user = request.getUser();
        if (user.getPears() < request.getAmount()) {
            return ResponseEntity.badRequest().body("Недостаточно груш у пользователя");
        }

        user.setPears(user.getPears() - request.getAmount());
        request.setStatus(WithdrawalStatus.APPROVED);

        userRepository.save(user);
        withdrawalRequestRepository.save(request);

        return ResponseEntity.ok("Заявка одобрена. Отправьте средства вручную на: " + request.getPaymentDetails());
    }

    @Operation(summary = "Отклонить заявку на вывод с указанием причины")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/withdrawals/reject")
    public ResponseEntity<String> rejectWithdrawal(
            @RequestParam Long requestId,
            @RequestParam String reason
    ) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            return ResponseEntity.badRequest().body("Заявка уже обработана");
        }

        request.setStatus(WithdrawalStatus.REJECTED);
        request.setRejectionReason(reason); // ✅ сохраняем причину
        withdrawalRequestRepository.save(request);

        return ResponseEntity.ok("Заявка отклонена. Причина: " + reason);
    }

    @Operation(summary = "Получить всех пользователей", description = "Этот endpoint возвращает список всех пользователей с пагинацией.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @Operation(summary = "Подтверждение рекламы (админ)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/approve-advertisement")
    public ResponseEntity<String> approveAdvertisement(@RequestParam Long adId) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        // Устанавливаем статус через enum
        ad.setStatus(AdvertisementStatus.APPROVED);
        // Добавляем установку статуса очереди, чтобы реклама попадала в очередь ожидания
        ad.setQueueStatus(AdQueueStatus.WAITING);
        advertisementRepository.save(ad);

        return ResponseEntity.ok("Реклама " + adId + " подтверждена администратором.");
    }
    @Operation(summary = "Отклонение рекламы (админ)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/reject-advertisement")
    public ResponseEntity<String> rejectAdvertisement(@RequestParam Long adId) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        // Устанавливаем статус через enum
        ad.setStatus(AdvertisementStatus.REJECTED);
        advertisementRepository.save(ad);

        return ResponseEntity.ok("Реклама " + adId + " отклонена администратором.");
    }
}
