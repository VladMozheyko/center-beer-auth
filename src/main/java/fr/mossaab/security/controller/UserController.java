package fr.mossaab.security.controller;


import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.MailSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Пользователь", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью user")
@RestController
@RequestMapping("/user")
@SecurityRequirements()
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final MailSender mailSender;


    @Operation(summary = "Получить профиль пользователя")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getSimpleProfile() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(user.getId()) // <-- добавлено
                .nickname(user.getNickname())
                .email(user.getEmail())
                .pears(user.getPears())
                .build();

        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Изменить никнейм текущего пользователя")
    @PatchMapping("/update-nickname")
    public ResponseEntity<String> updateNickname(@RequestParam String newNickname) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setNickname(newNickname);
        userRepository.save(user);
        return ResponseEntity.ok("Никнейм успешно обновлён на " + newNickname);
    }

    @Operation(summary = "Запросить смену e-mail (отправляет ссылку на новый адрес)")
    @PostMapping("/request-email-change")
    public ResponseEntity<String> requestEmailChange(@RequestParam String newEmail) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);
        user.setTempEmail(newEmail);
        userRepository.save(user);

        String confirmLink = "https://www.gwork.press:8443/user/confirm-email-change?code=" + activationCode;
        String message = "Здравствуйте! Перейдите по ссылке для подтверждения: \n" + confirmLink;
        mailSender.send(newEmail, "Подтверждение смены e-mail", message);

        return ResponseEntity.ok("Ссылка для подтверждения отправлена на " + newEmail);
    }

    @Operation(summary = "Подтвердить смену e-mail")
    @GetMapping("/confirm-email-change")
    public ResponseEntity<String> confirmEmailChange(@RequestParam String code) {
        User user = userRepository.findByActivationCode(code);
        if (user == null) {
            return ResponseEntity.badRequest().body("Неверный код подтверждения");
        }

        if (user.getTempEmail() == null || user.getTempEmail().isEmpty()) {
            return ResponseEntity.badRequest().body("Отсутствует новый e-mail для изменения");
        }

        // Проверяем, не занят ли новый адрес
        if (userRepository.findByEmail(user.getTempEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Данный e-mail уже используется другим пользователем");
        }

        String updatedEmail = user.getTempEmail();
        user.setEmail(updatedEmail);
        user.setTempEmail(null);
        user.setActivationCode(null);
        userRepository.save(user);

        return ResponseEntity.ok("E-mail успешно изменён на " + updatedEmail);
    }

}
