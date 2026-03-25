package fr.mossaab.security.controller;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileDataRepository fileDataRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() throws Exception {
        // Инициализируем базу данных тестовыми данными
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setNickname("testuser");
        user = userRepository.save(user);

        // Создание временного файла для тестирования
        File testImgFile = new File("upload-path/user_files/test.png");
        if (!testImgFile.exists()) {
            Files.createDirectories(testImgFile.getParentFile().toPath());
            Files.createFile(testImgFile.toPath());
        }

        FileData fileData = new FileData();
        fileData.setName("test.png");
        fileData.setFilePath(testImgFile.getPath());
        fileData.setUser(user);
        fileDataRepository.save(fileData);
    }

    @Test
    void downloadImageFromFileSystem_ShouldReturnImage() throws Exception {
        // Убедитесь, что существует файл и запись в базе данных.
        FileData fileData = new FileData();
        fileData.setName("test.png");
        fileData.setFilePath("upload-path/test.png");

        fileDataRepository.save(fileData);

        // Создайте файл, чтобы его можно было скачать.
        File file = new File("upload-path/test.png");
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            Files.createFile(file.toPath());
            Files.write(file.toPath(), "PNG image content".getBytes());
        }

        mockMvc.perform(get("/files/file-system/test.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().string(containsString("PNG")));
    }

    @Test
    void downloadPdfFromFileSystem_ShouldReturnPdf() throws Exception {
        // Аналогично методам, подготовьте файл PDF и выполните проверку
    }

    @Test
    void getUserProfileImage_ShouldReturnImage() throws Exception {
        mockMvc.perform(get("/files/profile-image/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().string(containsString("PNG")));
    }
}