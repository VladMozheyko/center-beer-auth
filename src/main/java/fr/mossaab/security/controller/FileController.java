package fr.mossaab.security.controller;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.StorageService;
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
@Tag(
        name = "Файлы",
        description = "API для загрузки изображений и PDF-документов из файловой системы по имени или идентификатору."
)
@RestController
@RequestMapping("/files")
@SecurityRequirements()
@RequiredArgsConstructor
public class FileController {
    private final StorageService storageService;
    private final FileDataRepository fileDataRepository;
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

    @Operation(summary = "Загрузка изображения профиля пользователя", description = "Этот endpoint позволяет загрузить изображения профиля в файловую систему")
    @PostMapping("/profile-image/upload/{userId}")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long userId,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        FileData fileData = (FileData) storageService.uploadImageToFileSystem(file, user);

        return ResponseEntity.status(HttpStatus.OK)
                .body(fileData);
    }

    @Operation(
            summary = "Получить изображение профиля пользователя",
            description = "Этот endpoint возвращает изображение профиля пользователя по userId."
    )
    @GetMapping("/profile-image/{userId}")
    public ResponseEntity<?> getUserProfileImage(@PathVariable Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        FileData fileData = user.getFileData();
        if (fileData == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageData = storageService.downloadImageFromFileSystem(fileData.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(fileData.getType()))
                .body(imageData);
    }
}
