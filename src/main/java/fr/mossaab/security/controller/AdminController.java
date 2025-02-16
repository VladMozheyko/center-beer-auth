package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.entities.User;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    //private static final Logger logger = LoggerFactory.getLogger(AdminController .class);

    @Operation(summary = "Получить всех пользователей", description = "Этот endpoint возвращает список всех пользователей с пагинацией.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<AdminService.GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
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

    @Operation(summary = "Получение логов сервер", description = "Этот endpoint возвращает логи с сервера.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/get-logs")
    public ResponseEntity<String> getLogs() throws IOException {
        return ResponseEntity.ok(adminService.getLogs());
    }

    @Operation(summary = "Показать подозрительные выигрыши за период")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/suspicious-wins")
    public ResponseEntity<List<UserSuspiciousDTO>> showSuspiciousWins(
            @RequestParam int limit
    ) {
        // Возвращаем список пользователей, у кого за последние X часов/дней общий выигрыш > limit
        // Логика сильно зависит от того, где и как вы храните транзакции/логи.

        // Пример "заглушка":
        List<User> allUsers = userRepository.findAll();
        List<UserSuspiciousDTO> suspicious = new ArrayList<>();
        for (User user : allUsers) {
            int pearsToday = getPearsToday(user); // надо считать из транзакций
            if (pearsToday > limit) {
                suspicious.add(new UserSuspiciousDTO(user.getId(), user.getNickname(), pearsToday));
            }
        }
        return ResponseEntity.ok(suspicious);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSuspiciousDTO {
        private Long userId;
        private String nickname;
        private int pearsObtained;
    }

    private int getPearsToday(User user) {
        // В реальности нужно смотреть историю: SELECT SUM(...) FROM pears_log WHERE user_id=? AND date>сегодня_полночь
        // Или вычислять иным образом.
        return 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class GetUsersDto {

        /**
         * Электронная почта пользователя.
         */
        private String email;


        /**
         * Номер телефона пользователя.
         */
        private String phoneNumber;

        /**
         * Роль работника.
         */
        private String workerRole;

        /**
         * Дата рождения пользователя.
         */
        private String dateOfBirth;

        /**
         * Фотография пользователя.
         */
        private String photo;

        /**
         * Код активации.
         */
        private Boolean activationCode;

        /**
         * Роль пользователя.
         */
        private String role;

        /**
         * Уникальный идентификатор пользователя.
         */
        private String id;
        private int balance;
    }
}
