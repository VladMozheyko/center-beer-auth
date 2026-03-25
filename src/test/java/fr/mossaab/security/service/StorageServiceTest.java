package fr.mossaab.security.service;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests для StorageService")
class StorageServiceTest {

    @InjectMocks
    private StorageService storageService;

    @Mock
    private FileDataRepository fileDataRepository;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "uploadBasePath", "upload-path");
    }

    @Nested
    @DisplayName("Tests for directory initialization")
    class DirectoryInitialization {

        @Test
        @DisplayName("Директории создаются, если их не существует")
        void directoriesAreCreatedIfNotExists() {
            new File("upload-path/user_files").delete();
            new File("upload-path/advertisement_files").delete();

            storageService.initDirectories();

            assertTrue(new File("upload-path/user_files").exists());
            assertTrue(new File("upload-path/advertisement_files").exists());
        }
    }

    @Nested
    @DisplayName("Tests for image uploading")
    class ImageUploading {

        @Test
        @DisplayName("Успешная загрузка изображения для пользователя")
        void uploadImage_Success_ForUser() throws IOException {
            User user = User.builder().build();
            when(multipartFile.isEmpty()).thenReturn(false);
            doNothing().when(multipartFile).transferTo(any(File.class));

            FileData fileData = FileData.builder()
                    .name(UUID.randomUUID().toString() + ".png")
                    .type("image/png")
                    .build();

            when(fileDataRepository.save(any(FileData.class))).thenReturn(fileData);

            FileData result = (FileData) storageService.uploadImageToFileSystem(multipartFile, user);

            assertNotNull(result);
            verify(multipartFile).transferTo(any(File.class));
            verify(fileDataRepository).save(any(FileData.class));
        }

        @Test
        @DisplayName("Несуществующий тип сущности вызывает исключение")
        void uploadImage_InvalidEntityType_ThrowsException() {
            Object unsupportedEntity = new Object(); // Сущность неизвестного типа

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    storageService.uploadImageToFileSystem(multipartFile, unsupportedEntity));

            assertEquals("Unsupported related entity type: Object", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Tests for image downloading")
    class ImageDownloading {

        @Test
        @DisplayName("Успешная загрузка изображения из файловой системы")
        void downloadImage_Success(@TempDir Path tempDir) throws IOException {
            Path tempFilePath = tempDir.resolve("test-img.png");
            Files.createFile(tempFilePath); // Создаем временный файл

            FileData fileData = FileData.builder()
                    .name("test-img.png")
                    .filePath(tempFilePath.toString())
                    .build();
            when(fileDataRepository.findByName("test-img.png")).thenReturn(Optional.of(fileData));

            // Предполагаем существование файла и корректное выполнение
            byte[] data = storageService.downloadImageFromFileSystem("test-img.png");

            assertNotNull(data);
        }

        @Test
        @DisplayName("Отсутствующий файл вызывает исключение")
        void downloadImage_FileNotExists_ThrowsException() {
            when(fileDataRepository.findByName("nonexistent.png")).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    storageService.downloadImageFromFileSystem("nonexistent.png"));

            assertEquals("Файл не найден", exception.getMessage());
        }
    }
}