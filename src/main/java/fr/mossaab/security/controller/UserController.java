package fr.mossaab.security.controller;

import fr.mossaab.security.dto.payment.WithdrawalStatus;
import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;
    @Operation(summary = "Мои заявки на вывод")
    @GetMapping("/my-withdrawals")
    public ResponseEntity<List<WithdrawalRequest>> getMyWithdrawals() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<WithdrawalRequest> list = withdrawalRequestRepository.findAll().stream()
                .filter(req -> req.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Создать заявку на вывод средств")
    @PostMapping("/withdraw")
    public ResponseEntity<String> createWithdrawalRequest(
            @RequestParam String paymentDetails,
            @RequestParam Integer amount
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getPears() < amount) {
            return ResponseEntity.badRequest().body("Недостаточно груш для вывода");
        }

        WithdrawalRequest request = WithdrawalRequest.builder()
                .user(user)
                .paymentDetails(paymentDetails)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        withdrawalRequestRepository.save(request);

        return ResponseEntity.ok("Заявка на вывод создана и ожидает подтверждения администратором.");
    }

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
    @Operation(summary = "Начисление груш за мини игру")
    @PostMapping("/catch-pear")
    public ResponseEntity<String> catchPear(@RequestParam Long userId, @RequestParam int pearsCaught) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверка, не превышено ли "макс. возможное".
        // Допустим, создадим метод user.canCatchMorePears(pearsCaught).
        if (!canCatchMorePears(user, pearsCaught)) {
            // Либо пишем в лог подозрительные действия
            return ResponseEntity.badRequest().body("Превышен лимит на количество груш в этой мини-игре");
        }

        // Увеличиваем pears (возможно, временно)
        user.setPears(user.getPears() + pearsCaught);
        userRepository.save(user);

        return ResponseEntity.ok("Груши успешно начислены пользователю " + user.getNickname());
    }

    private boolean canCatchMorePears(User user, int pearsCaught) {
        // Тут любая своя логика, например:
        // - За одну сессию игры нельзя поймать больше 100 груш
        // - Или сверяться с user.getMaxPossiblePearsThisGame() и т.п.
        return true;
    }
}
