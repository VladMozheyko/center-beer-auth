package fr.mossaab.security.unit.controller;

import fr.mossaab.security.controller.FileController;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-tests for FileController")
class FileControllerTest {

    @InjectMocks
    private FileController fileController;

    @Mock
    private StorageService storageService;

    @Mock
    private FileDataRepository fileDataRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        // Настройка перед выполнением каждого теста
    }

    @Test
    @DisplayName("Загрузка PDF из файловой системы по имени - успешная")
    void downloadPdfFromFileSystem_Success() throws IOException {
        when(storageService.downloadImageFromFileSystem("test.pdf")).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<?> response = fileController.downloadPdfFromFileSystem("test.pdf");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        verify(storageService, times(1)).downloadImageFromFileSystem("test.pdf");
    }

    @Test
    @DisplayName("Загрузка изображения из файловой системы по имени - успешная")
    void downloadImageFromFileSystem_Success() throws IOException {
        when(storageService.downloadImageFromFileSystem("test.png")).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<?> response = fileController.downloadImageFromFileSystem("test.png");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("image/png", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        verify(storageService, times(1)).downloadImageFromFileSystem("test.png");
    }

    @Test
    @DisplayName("Загрузка PDF по идентификатору - успешная")
    void downloadPdfById_Success() throws IOException {
        FileData fileData = new FileData(null, "test.pdf", "application/pdf", "/path/to/test.pdf", null);

        when(fileDataRepository.findById(1L)).thenReturn(Optional.of(fileData));
        when(storageService.downloadImageFromFileSystem(fileData.getName())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<?> response = fileController.downloadPdfById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        verify(storageService, times(1)).downloadImageFromFileSystem(fileData.getName());
    }

    @Test
    @DisplayName("Загрузка изображения по идентификатору - успешная")
    void downloadImageById_Success() throws IOException {
        FileData fileData = new FileData(null, "test.png", "image/png", "/path/to/test.png", null);

        when(fileDataRepository.findById(1L)).thenReturn(Optional.of(fileData));
        when(storageService.downloadImageFromFileSystem(fileData.getName())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<?> response = fileController.downloadImageById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("image/png", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        verify(storageService, times(1)).downloadImageFromFileSystem(fileData.getName());
    }

    @Test
    @DisplayName("Загрузка изображения профиля пользователя - успешная")
    void uploadProfileImage_Success() throws IOException {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        FileData fileData = new FileData(1L, "image.png", "image/png", "/path/to/image.png", user);
        when(storageService.uploadImageToFileSystem(any(MultipartFile.class), any(User.class))).thenReturn(fileData);

        ResponseEntity<?> response = fileController.uploadProfileImage(1L, multipartFile);

        assertEquals(200, response.getStatusCode().value());
        verify(storageService, times(1)).uploadImageToFileSystem(multipartFile, user);
    }

    @Test
    @DisplayName("Получение изображения профиля пользователя - успешное")
    void getUserProfileImage_Success() throws IOException {
        FileData fileData = new FileData(null, "profile.png", "image/png", "/path/to/profile.png", null);
        User user = new User();
        user.setFileData(fileData);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storageService.downloadImageFromFileSystem(fileData.getName())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<?> response = fileController.getUserProfileImage(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("image/png", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        verify(storageService, times(1)).downloadImageFromFileSystem(fileData.getName());
    }

    @Test
    @DisplayName("Файл не найден по идентификатору - вызывает исключение")
    void downloadFileById_NotFound() {
        when(fileDataRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                fileController.downloadPdfById(1L));

        assertEquals("Файл с указанным идентификатором не найден", exception.getMessage());
    }

    @Test
    @DisplayName("Пользователь не найден при загрузке изображения профиля - вызывает исключение")
    void uploadProfileImage_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                fileController.uploadProfileImage(1L, multipartFile));

        assertEquals("Пользователь не найден", exception.getMessage());
    }
}