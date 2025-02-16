package fr.mossaab.security.controller;

import fr.mossaab.security.entities.*;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Пользователь", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью user")
@RestController
@RequestMapping("/user")
@SecurityRequirements()
@RequiredArgsConstructor
public class UserController {
    private final FileDataRepository fileDataRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final MailSender mailSender; // Добавили
    //private final Drive driveService;
    @Operation(summary = "Загрузка PDF-файла из файловой системы", description = "Этот endpoint позволяет загрузить PDF-файл из файловой системы.")
    @GetMapping("/file-system-pdf/{fileName}")
    public ResponseEntity<?> downloadPdfFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] pdfData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("application/pdf"))
                .body(pdfData);
    }

    @Operation(summary = "Загрузка изображения из файловой системы", description = "Этот endpoint позволяет загрузить изображение из файловой системы.")
    @GetMapping("/file-system/{fileName}")
    public ResponseEntity<?> downloadImageFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Загрузка PDF-файла по идентификатору", description = "Этот endpoint позволяет загрузить PDF-файл по идентификатору.")
    @GetMapping("/file-system-pdf-by-id/{fileDataId}")
    public ResponseEntity<?> downloadPdfById(@PathVariable Long fileDataId) throws IOException {
        FileData fileData = fileDataRepository.findById(fileDataId)
                .orElseThrow(() -> new RuntimeException("Файл с указанным идентификатором не найден"));

        byte[] pdfData = storageService.downloadImageFromFileSystem(fileData.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("application/pdf"))
                .body(pdfData);
    }
    @Operation(summary = "Загрузка изображения по идентификатору", description = "Этот endpoint позволяет загрузить изображение по идентификатору.")
    @GetMapping("/file-system-image-by-id/{fileDataId}")
    public ResponseEntity<?> downloadImageById(@PathVariable Long fileDataId) throws IOException {
        FileData fileData = fileDataRepository.findById(fileDataId)
                .orElseThrow(() -> new RuntimeException("Файл с указанным идентификатором не найден"));

        byte[] imageData = storageService.downloadImageFromFileSystem(fileData.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }

    @Operation(summary = "Получить профиль", description = "Этот эндпоинт возвращает профиль пользователя на основе предоставленного куки.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(HttpServletRequest request) {
        // Получаем email пользователя из контекста безопасности
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // Ищем пользователя в базе данных
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Создаем ответ с профилем пользователя
        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .pears(user.getPears())
                .build();

        // Создаем API-ответ с метаинформацией
        ApiResponse<UserProfileResponse> apiResponse = ApiResponse.<UserProfileResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Профиль успешно получен")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .user(user.getEmail())
                .data(profileResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
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

        String confirmLink = "http://158.160.138.117:8080/user/confirm-email-change?code=" + activationCode;
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


    // DTO для ответа с профилем пользователя
    @Data
    @Builder
    public static class UserProfileResponse {
        private String nickname;
        private String email;
        private Integer pears;
    }



    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetUserResponse {
        private String status;
        private Object answer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private String role;
        private String typeOfWorker;
        private String firstName;
        private String lastName;
        private String photo;
    }
    @Getter
    @Setter
    public static class ResponseGetProfile {
        private String status;
        private String notify;
        private AnswerGetProfile answer;
    }
    @Data
    public static class AnswerGetProfile {
        private String phone;
        private String dateOfBirth;
        private String typeOfWorker;
        private String firstName;
        private String lastName;
        private String email;
        private String photo;
        private Long userId;
        private int balance; // Баланс пользователя
        private boolean isVerified; // Статус верификации документов
    }
    @Data
    public class EditProfileDto {
        private MultipartFile image;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
        private String dateOfBirth;
    }
}
