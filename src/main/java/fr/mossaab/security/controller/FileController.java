package fr.mossaab.security.controller;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
