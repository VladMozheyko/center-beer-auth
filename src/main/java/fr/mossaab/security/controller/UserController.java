package fr.mossaab.security.controller;

import fr.mossaab.security.dto.request.EditProfileDto;
import fr.mossaab.security.dto.response.*;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.RefreshTokenRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.impl.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
@Tag(name = "User", description = "Контроллер предоставляющие методы доступные пользователю с ролью user")
@RestController
@RequestMapping("/user")
@SecurityRequirements()
@RequiredArgsConstructor
public class UserController {
    private final FileDataRepository fileDataRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;

    @Operation(summary = "Загрузка изображения аватарки пользователя из файловой системы", description = "Этот эндпоинт позволяет загрузить изображение аватарки пользователя из файловой системы.")
    @GetMapping("/fileSystem/{fileName}")
    public ResponseEntity<?> downloadImageFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = storageService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }

    @Operation(summary = "Получить данные пользователя", description = "Этот эндпоинт возвращает данные пользователя на основе предоставленного куки.")
    @GetMapping("/user")
    public ResponseEntity<GetUserResponse> getUser(@CookieValue("refresh-jwt-cookie") String cookie) {
        GetUserResponse response = new GetUserResponse();
        UserDTO userDTO = new UserDTO();
        List<FileData> allFileData = fileDataRepository.findAll();
        String fileDataPath = null;
        for (FileData fileData : allFileData) {
            if (fileData.getUser().getId() == refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getId()) {
                fileDataPath = fileData.getName();
                break;
            }
        }

        User user = refreshTokenRepository.findByToken(cookie).orElse(null).getUser();

        if (user != null) {
            userDTO.setRole(user.getRole().toString());
            userDTO.setTypeOfWorker(user.getWorkerRoles().toString());
            userDTO.setFirstName(user.getFirstname());
            userDTO.setLastName(user.getLastname());
            userDTO.setPhoto(fileDataPath);

            response.setStatus("success");
            response.setAnswer(userDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.setStatus("error");
            response.setAnswer("User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Получить профиль", description = "Этот эндпоинт возвращает профиль пользователя на основе предоставленного куки.")
    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile(@CookieValue("refresh-jwt-cookie") String cookie) {
        ResponseGetProfile response = new ResponseGetProfile();
        AnswerGetProfile answer = new AnswerGetProfile();
        response.setStatus("success");
        response.setNotify("Профиль получен");

        List<FileData> allFileData = fileDataRepository.findAll();
        String fileDataPath = null;
        Long userId = refreshTokenRepository.findByToken(cookie).orElse(null).getUser().getId();

        for (FileData fileData : allFileData) {
            if (fileData.getUser().getId().equals(userId)) {
                fileDataPath = fileData.getName();
                break;
            }
        }

        var user = refreshTokenRepository.findByToken(cookie).orElse(null).getUser();
        answer.setPhone(user.getPhoneNumber());
        answer.setDateOfBirth(user.getDateOfBirth().toString());
        answer.setTypeOfWorker(user.getWorkerRoles().toString());
        answer.setFirstName(user.getFirstname());
        answer.setLastName(user.getLastname());
        answer.setEmail(user.getEmail());
        answer.setPhoto(fileDataPath);

        response.setAnswer(answer);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Редактирование профиля", description = "Этот эндпоинт позволяет пользователю изменить свой профиль.")
    @PostMapping("/editProfile")
    public ResponseEntity<Object> editProfile(@CookieValue("refresh-jwt-cookie") String cookie,
                                              @RequestPart EditProfileDto editProfileDto,
                                              @RequestPart MultipartFile image) throws ParseException, IOException {
        User user = refreshTokenRepository.findByToken(cookie).orElse(null).getUser();
        EditProfileResponse response = new EditProfileResponse();
        ErrorsEditProfileDto errors = new ErrorsEditProfileDto();

        response.setStatus("success");
        response.setNotify("Изменения выполнены");
        response.setAnswer("edit success");

        errors.setFirstname("");
        errors.setLastname("");
        errors.setDateOfBirth("");
        errors.setPhoto("");

        // Логика проверки и заполнения errors
        if (editProfileDto.getUsername() == null || editProfileDto.getUsername().isEmpty()) {
            errors.setFirstname("Неправильное имя пользователя");
        }
        if (editProfileDto.getLastname() == null || editProfileDto.getLastname().isEmpty()) {
            errors.setLastname("Неправильная фамилия");
        }
        if (editProfileDto.getDateOfBirth() == null) {
            errors.setDateOfBirth("Неверная дата рождения");
        }
        if (image == null || image.isEmpty()) {
            errors.setPhoto("Неверная фотография");
        }

        int count = 0;
        for (Field field : errors.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.get(errors) != null && !field.get(errors).toString().isEmpty()) {
                    count++;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (count == 0) {
            response.setErrors(errors);

            user.setFirstname(editProfileDto.getUsername());
            user.setLastname(editProfileDto.getLastname());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            user.setDateOfBirth(format.parse(editProfileDto.getDateOfBirth()));
            userRepository.save(user);

            List<FileData> allFileData = fileDataRepository.findAll();
            String fileDataPath = null;
            for (FileData fileData : allFileData) {
                if (fileData.getUser().getId() == user.getId()) {
                    fileDataPath = fileData.getName();
                    break;
                }
            }

            if (fileDataPath != null) {
                fileDataRepository.deleteByName(fileDataPath);
            }

            FileData uploadImage = storageService.uploadImageToFileSystemAvatarUser(image, user);
            fileDataRepository.save(uploadImage);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.setStatus("error");
        response.setNotify("В изменениях отказано");
        response.setAnswer("edit error");
        response.setErrors(errors);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
