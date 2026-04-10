package fr.mossaab.security.controller;


import fr.mossaab.security.dto.user.GetAllUsersResponse;
import fr.mossaab.security.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Operation(summary = "Получить всех пользователей", description = "Этот endpoint возвращает список всех пользователей с пагинацией.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Номер страницы не может быть меньше 0")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Размер страницы должен быть минимум 1")
            @Max(value = 100, message = "Размер страницы не может превышать 100")
            int size
    ) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

}
