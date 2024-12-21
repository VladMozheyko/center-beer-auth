package fr.mossaab.security.controller;

import fr.mossaab.security.entities.*;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Date;
import java.util.*;

@Tag(name = "Пользователь", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью user")
@RestController
@RequestMapping("/user")
@SecurityRequirements()
@RequiredArgsConstructor
public class UserController {
    private final FileDataRepository fileDataRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final MailSender mailSender;

    @Operation(summary = "Загрузка PDF-файла из файловой системы", description = "Этот эндпоинт позволяет загрузить PDF-файл из файловой системы.")
    @GetMapping("/fileSystemPdf/{fileName}")
    public ResponseEntity<?> downloadPdfFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] pdfData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("application/pdf"))
                .body(pdfData);
    }

    @Operation(summary = "Загрузка изображения из файловой системы", description = "Этот эндпоинт позволяет загрузить изображение из файловой системы.")
    @GetMapping("/fileSystem/{fileName}")
    public ResponseEntity<?> downloadImageFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Загрузка PDF-файла по идентификатору", description = "Этот эндпоинт позволяет загрузить PDF-файл по идентификатору.")
    @GetMapping("/fileSystemPdfById/{fileDataId}")
    public ResponseEntity<?> downloadPdfById(@PathVariable Long fileDataId) throws IOException {
        FileData fileData = fileDataRepository.findById(fileDataId)
                .orElseThrow(() -> new RuntimeException("Файл с указанным идентификатором не найден"));

        byte[] pdfData = storageService.downloadImageFromFileSystem(fileData.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("application/pdf"))
                .body(pdfData);
    }
    @Operation(summary = "Загрузка изображения по идентификатору", description = "Этот эндпоинт позволяет загрузить изображение по идентификатору.")
    @GetMapping("/fileSystemImageById/{fileDataId}")
    public ResponseEntity<?> downloadImageById(@PathVariable Long fileDataId) throws IOException {
        FileData fileData = fileDataRepository.findById(fileDataId)
                .orElseThrow(() -> new RuntimeException("Файл с указанным идентификатором не найден"));

        byte[] imageData = storageService.downloadImageFromFileSystem(fileData.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    @Operation(summary = "Получить данные пользователя", description = "Этот эндпоинт возвращает данные пользователя на основе предоставленного куки.")
    @GetMapping("/user")
    public ResponseEntity<GetUserResponse> getUser(@CookieValue("refresh-jwt-cookie") String cookie) {
        GetUserResponse response = new GetUserResponse();
        UserDTO userDTO = new UserDTO();

        // Поиск refresh-токена и связанного пользователя
        User user = refreshTokenRepository.findByToken(cookie)
                .map(RefreshToken::getUser)
                .orElse(null);

        if (user == null) {
            response.setStatus("error");
            response.setAnswer("User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        // Ищем фото пользователя, если оно существует
        String fileDataPath = null;
        List<FileData> allFileData = fileDataRepository.findAll();

        for (FileData fileData : allFileData) {
            if (fileData != null && fileData.getUser() != null && fileData.getUser().getId() == user.getId()) {
                fileDataPath = fileData.getName();
                break;
            }
        }

        // Заполняем данные пользователя
        userDTO.setRole(user.getRole().toString());
        userDTO.setFirstName(user.getFirstname());
        userDTO.setLastName(user.getLastname());
        userDTO.setPhoto(fileDataPath);

        response.setStatus("success");
        response.setAnswer(userDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "Получить профиль", description = "Этот эндпоинт возвращает профиль пользователя на основе предоставленного куки.")
    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile(@CookieValue("refresh-jwt-cookie") String cookie) {
        ResponseGetProfile response = new ResponseGetProfile();
        AnswerGetProfile answer = new AnswerGetProfile();
        response.setStatus("success");
        response.setNotify("Профиль получен");

        // Ищем ID пользователя по куки
        Long userId = refreshTokenRepository.findByToken(cookie)
                .map(RefreshToken::getUser)
                .map(User::getId)
                .orElse(null);

        if (userId == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        // Ищем фото пользователя, если оно существует
        String fileDataPath = null;
        List<FileData> allFileData = fileDataRepository.findAll();

        for (FileData fileData : allFileData) {
            if (fileData != null && fileData.getUser() != null && fileData.getUser().getId().equals(userId)) {
                fileDataPath = fileData.getName();
                break;
            }
        }

        // Получаем пользователя
        User user = refreshTokenRepository.findByToken(cookie)
                .map(RefreshToken::getUser)
                .orElse(null);

        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        // Проверяем статус верификации документов

        // Заполняем данные профиля
        answer.setPhone(user.getPhoneNumber());
        answer.setDateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
        answer.setFirstName(user.getFirstname());
        answer.setLastName(user.getLastname());
        answer.setEmail(user.getEmail());
        answer.setPhoto(fileDataPath);
        answer.setUserId(user.getId());
        answer.setBalance(user.getBalance()); // Установка баланса пользователя

        response.setAnswer(answer);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @Operation(summary = "Отправка запроса на подтверждение изменения профиля через почту")
    @PostMapping(value = "/edit-profile", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> editProfile(@RequestPart EditProfileDto request, @RequestPart(required = false) MultipartFile image, @CookieValue("refresh-jwt-cookie") String cookie) throws IOException {
        User user = refreshTokenRepository.findByToken(cookie).orElse(null).getUser();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        Map<String, String> changes = user.getProposedChanges().getChanges();
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            changes.put("firstName", request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            changes.put("lastName", request.getLastName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            changes.put("phoneNumber", request.getPhoneNumber());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            changes.put("email", request.getEmail());
        }
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
            changes.put("dateOfBirth", request.getDateOfBirth());
        }

        if (image != null && !image.isEmpty()) {
            FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(image, user);
            fileDataRepository.save(uploadImage);
            user.getProposedChanges().setProposedPhoto(uploadImage);
        }

        userRepository.save(user);

        // Отправка кода подтверждения на почту пользователя
        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);
        userRepository.save(user);

        String message = String.format(
                "Здравствуйте, %s! \n" +
                        "Для подтверждения изменений профиля перейдите по этой ссылке: http://31.129.102.70:8080/user/confirm-changes/%s",
                user.getFirstname(),
                user.getActivationCode()
        );

        mailSender.send(user.getEmail(), "Подтверждение изменений профиля", message);

        return ResponseEntity.ok("Изменения предложены, подтвердите изменения через код, отправленный на вашу почту.");
    }
    @Operation(summary = "Подтверждение изменений профиля")
    @PostMapping("/confirm-changes/{code}")
    public ResponseEntity<Object> confirmChanges(@PathVariable String code) {
        User user = userRepository.findByActivationCode(code);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid code");
        }

        Map<String, String> changes = user.getProposedChanges().getChanges();
        if (changes.containsKey("firstName")) {
            user.setFirstname(changes.get("firstName"));
        }
        if (changes.containsKey("lastName")) {
            user.setLastname(changes.get("lastName"));
        }
        if (changes.containsKey("phoneNumber")) {
            user.setPhoneNumber(changes.get("phoneNumber"));
        }
        if (changes.containsKey("email")) {
            user.setEmail(changes.get("email"));
        }
        if (changes.containsKey("dateOfBirth")) {
            user.setDateOfBirth(Date.valueOf(changes.get("dateOfBirth")));
        }

        FileData proposedPhoto = user.getProposedChanges().getProposedPhoto();
        if (proposedPhoto != null) {
            user.setFileData(proposedPhoto);
        }

        user.setProposedChanges(new ProposedChanges());
        user.setActivationCode(null);
        userRepository.save(user);

        return ResponseEntity.ok("Изменения профиля успешно подтверждены.");
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
