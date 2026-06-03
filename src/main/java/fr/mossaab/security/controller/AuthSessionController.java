package fr.mossaab.security.controller;


import fr.mossaab.security.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Tag(name = "Аутентификация", description = "API для работы с аутентификацией пользователей")
@RestController
@RequestMapping("/auth-session")
@SecurityRequirements
@RequiredArgsConstructor
public class AuthSessionController {

    private final AuthenticationService authenticationService;

    @Operation(
            summary = "Выход из системы 🚨(new)",
            description = "Этот endpoint позволяет пользователю выйти из системы, только с этого устройства",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный выход пользователя",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован",
                            content = @Content
                    )
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        return authenticationService.logout(request);
    }

    @Operation(
            summary = "Выход со всех устройств (включая выключая это устройство) 🚨(new)",
            description = "Этот endpoint позволяет пользователю выйти из системы на всех устройствах (удаляет все refresh-токены пользователя если флаг установлен true, иначе выход будет произведен со всех устройств кроме этого).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный выход со всех устройств",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован",
                            content = @Content
                    )
            }
    )
    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutFromAllDevices(
            HttpServletRequest request,
            @Parameter(description = "Флаг, указывающий, выходит ли пользователь и с этого устройства", example = "false")
            @RequestParam(defaultValue = "false") boolean loggingOutOfThisDevice) {
        return authenticationService.logoutAllDevices(request, loggingOutOfThisDevice);
    }

    @Operation(
            summary = "Список активных сессий/устройств 🚨(new)",
            description = "Возвращает список активных сессий пользователя (refresh-токены).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список активных сессий",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionInfoResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован",
                            content = @Content
                    )
            }
    )
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfoResponse>> getActiveSessions() {
        return authenticationService.getActiveSessions();
    }
}
