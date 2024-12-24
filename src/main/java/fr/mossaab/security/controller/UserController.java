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

@Tag(name = "Пользователь", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью user")
@RestController
@RequestMapping("/user")
@SecurityRequirements()
@RequiredArgsConstructor
public class UserController {
    private final FileDataRepository fileDataRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;

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
