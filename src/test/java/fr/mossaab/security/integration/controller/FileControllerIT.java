package fr.mossaab.security.integration.controller;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileDataRepository fileDataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        // Создаём временную директорию и устанавливаем её в StorageService
        tempDir = Files.createTempDirectory("test-uploads");
        setUploadBasePath(tempDir.toString());

        // Инициализируем базу данных тестовыми данными
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setNickname("testuser");
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        User user2 = new User();
        user2.setEmail("testuser2@example.com");
        user2.setNickname("testuser2");
        user2.setCreatedAt(LocalDateTime.now());
        user2 = userRepository.save(user2);

        // Создание временного файла для тестирования
        File testImgFile = new File(tempDir + "/user_files/test.png");
        if (!testImgFile.exists()) {
            Files.createDirectories(testImgFile.getParentFile().toPath());
            Files.createFile(testImgFile.toPath());
            Files.write(testImgFile.toPath(), "PNG image content".getBytes());
        }

        File testPdfFile = new File(tempDir + "/user_files/test.pdf");
        if (!testPdfFile.exists()) {
            Files.createFile(testPdfFile.toPath());
            Files.write(testPdfFile.toPath(), "%PDF-1.4".getBytes());
        }

        FileData fileDataImg = new FileData();
        fileDataImg.setName("test1");
        fileDataImg.setType("image/png");
        fileDataImg.setFilePath(testImgFile.getPath());
        fileDataImg.setUser(user);
        fileDataRepository.save(fileDataImg);
        user.setFileData(fileDataImg);
        userRepository.save(user);

        FileData fileDataPdf = new FileData();
        fileDataPdf.setName("test2");
        fileDataPdf.setType("application/pdf");
        fileDataPdf.setFilePath(testPdfFile.getPath());
        fileDataPdf.setUser(user2);
        fileDataRepository.save(fileDataPdf);
        user2.setFileData(fileDataPdf);
        userRepository.save(user);
    }

    private void setUploadBasePath(String path) throws NoSuchFieldException, IllegalAccessException {
        Field uploadPathField = StorageService.class.getDeclaredField("uploadBasePath");
        uploadPathField.setAccessible(true);
        uploadPathField.set(storageService, path);
    }

    @Test
    @DisplayName("Получение изображения профиля пользователя")
    void getUserProfileImage_ShouldReturnImage() throws Exception {
        Long userId = userRepository.findByEmail("testuser@example.com").orElseThrow().getId();

        mockMvc.perform(get("/files/profile-image/" + userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @DisplayName("Загрузка изображения профиля пользователя")
    void uploadProfileImage_ShouldUploadImage() throws Exception {
        Long userId = userRepository.findByEmail("testuser@example.com").orElseThrow().getId();
        Path imgFilePath = Files.createTempFile(tempDir, "imgfile", ".png");
        Files.write(imgFilePath, "PNG content".getBytes());

        mockMvc.perform(multipart("/files/profile-image/upload/" + userId)
                        .file("file", Files.readAllBytes(imgFilePath))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }
}