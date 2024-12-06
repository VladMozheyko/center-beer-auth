package fr.mossaab.security.controller;

import fr.mossaab.security.dto.response.GetAllUsersResponse;
import fr.mossaab.security.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Tag(name = "Администратор", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью администратор")
@RestController
@RequestMapping("/admin")
@SecurityRequirements()
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private static final String LOG_FILE_PATH = "/root/kotitonttu/log.txt";
    @Operation(summary = "Получить всех пользователей", description = "Этот эндпоинт возвращает список всех пользователей с пагинацией.")
    @GetMapping("/allUsers")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @Operation(summary = "Вернуть логи", description = "Вернуть логи сервера")
    @GetMapping("/get-logs")
    public ResponseEntity<String> getLogs() {
        try {
            // Читаем содержимое файла
            Path logPath = Paths.get(LOG_FILE_PATH);
            String logs = Files.readString(logPath);

            // Возвращаем содержимое
            return ResponseEntity.ok(logs);
        } catch (IOException e) {
            // Ловим ошибки и возвращаем сообщение
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Не удалось прочитать логи: " + e.getMessage());
        }
    }
}
