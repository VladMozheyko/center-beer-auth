package fr.mossaab.security.controller;

import fr.mossaab.security.backup.BackupService;
import fr.mossaab.security.dto.user.GetAllUsersResponse;
import fr.mossaab.security.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Администратор", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью администратор")
@RestController
@RequestMapping("/admin")
@SecurityRequirements()
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final BackupService backupService;
    @Operation(summary = "Получить всех пользователей", description = "Этот endpoint возвращает список всех пользователей с пагинацией.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }
    @PostMapping("/backup-now")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> backupNow() {
        backupService.save();
        return ResponseEntity.ok("backup.json сохранён");
    }
    @PostMapping("/restore-now")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> restoreNow() {
        try {
            backupService.restoreIfExists();
            return ResponseEntity.ok("Данные успешно восстановлены из backup.json");
        } catch (Exception ex) {
            return ResponseEntity
                    .internalServerError()
                    .body("Ошибка при восстановлении: " + ex.getMessage());
        }
    }
}
