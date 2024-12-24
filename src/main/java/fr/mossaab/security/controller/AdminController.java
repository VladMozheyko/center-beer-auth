package fr.mossaab.security.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Администратор", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью администратор")
@RestController
@RequestMapping("/admin")
@SecurityRequirements()
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    //private static final Logger logger = LoggerFactory.getLogger(AdminController .class);

    @Operation(summary = "Получить всех пользователей", description = "Этот endpoint возвращает список всех пользователей с пагинацией.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all-users")
    public ResponseEntity<AdminService.GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @Operation(summary = "Получение логов сервер", description = "Этот endpoint возвращает логи с сервера.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/get-logs")
    public ResponseEntity<String> getLogs() throws IOException {
        return ResponseEntity.ok(adminService.getLogs());
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
