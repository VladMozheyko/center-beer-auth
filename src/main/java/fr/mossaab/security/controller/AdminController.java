package fr.mossaab.security.controller;

import com.sun.management.OperatingSystemMXBean;
import fr.mossaab.security.dto.user.GetAllUsersResponse;
import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.repository.AdvertisementRepository;
import fr.mossaab.security.repository.UserRepository;
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
    @Operation(summary = "Получить всех пользователей", description = "Этот endpoint возвращает список всех пользователей с пагинацией.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @Operation(summary = "Начислить (или списать) пользователю груши по команде админа")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/adjust-pears")
    public ResponseEntity<String> adjustPears(
            @RequestParam Long userId,
            @RequestParam Integer amount // может быть положительным или отрицательным
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Можно добавить любую доп. логику проверки (например, лимиты)
        int oldPears = user.getPears();
        user.setPears(oldPears + amount);
        userRepository.save(user);

        return ResponseEntity.ok("Пользователю " + user.getNickname() +
                " успешно добавлено " + amount + " груш. Итого: " + user.getPears());
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
