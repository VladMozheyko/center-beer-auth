package fr.mossaab.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер авторизации для работы с защищенными ресурсами.
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
@Tag(name = "Authorization", description = "API авторизации. Содержит защищенный метод приветствия")
public class AuthorizationController {

    /**
     * GET-метод для получения приветствия с ролью ADMIN и правами на чтение.
     *
     * @return Ответ с приветствием и статусом 200 OK.
     */
    @GetMapping("/admin/resource")
    @PreAuthorize("hasAuthority('READ_PRIVILEGE') and hasRole('ADMIN')")
    @Operation(
            description = "Этот конечный пункт требует действительного JWT, роли ADMIN с READ_PRIVILEGE",
            summary = "Защищенный конечный пункт приветствия",
            responses = {
                    @ApiResponse(
                            description = "Успешно",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Несанкционированно / Неверный токен",
                            responseCode = "401"
                    )
            }
    )
    public ResponseEntity<String> sayHelloWithRoleAdminAndReadAuthority() {
        return ResponseEntity.ok("Привет, у вас есть доступ к защищенному ресурсу, требующему роль администратора и права на чтение.");
    }

    /**
     * DELETE-метод для получения приветствия с ролью ADMIN и правами на удаление.
     *
     * @return Ответ с приветствием и статусом 200 OK.
     */
    @DeleteMapping("/admin/resource")
    @PreAuthorize("hasAuthority('DELETE_PRIVILEGE') and hasRole('ADMIN')")
    public ResponseEntity<String> sayHelloWithRoleAdminAndDeleteAuthority() {
        return ResponseEntity.ok("Привет, у вас есть доступ к защищенному ресурсу, требующему роль администратора и права на удаление.");
    }

    /**
     * POST-метод для получения приветствия с ролью USER и правами на создание.
     *
     * @return Ответ с приветствием и статусом 200 OK.
     */
    @PostMapping("/user/resource")
    @PreAuthorize("hasAuthority('WRITE_PRIVILEGE') and hasAnyRole('ADMIN','USER')")
    public ResponseEntity<String> sayHelloWithRoleUserAndCreateAuthority() {
        return ResponseEntity.ok("Привет, у вас есть доступ к защищенному ресурсу, требующему роль пользователя и права на создание.");
    }

    /**
     * PUT-метод для получения приветствия с ролью USER и правами на обновление.
     *
     * @return Ответ с приветствием и статусом 200 OK.
     */
    @PutMapping("/user/resource")
    @PreAuthorize("hasAuthority('UPDATE_PRIVILEGE') and hasAnyRole('ADMIN','USER')")
    public ResponseEntity<String> sayHelloWithRoleUserAndUpdateAuthority() {
        return ResponseEntity.ok("Привет, у вас есть доступ к защищенному ресурсу, требующему роль пользователя и права на обновление.");
    }

}
