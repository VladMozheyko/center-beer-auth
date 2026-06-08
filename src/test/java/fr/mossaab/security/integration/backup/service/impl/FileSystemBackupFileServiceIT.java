package fr.mossaab.security.integration.backup.service.impl;

import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.impl.FileSystemBackupFileService;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сервиса работы с файлами бэкапов в файловой системе
 */
@DisplayName("Интеграционные тесты - FileSystemBackupFileService")
@SpringBootTest
@TestPropertySource(properties = "backup.file-system.base-path=${java.io.tmpdir}/test-backup")
class FileSystemBackupFileServiceIT extends AbstractIntegrationTest {

    @TempDir
    Path sharedTempDir;

    @Autowired
    private FileSystemBackupFileService fileSystemBackupFileService;

    @SpyBean
    private BackupProperties backupProperties;

    @BeforeEach
    void setupPath() {
        backupProperties.getFileSystem().setBasePath(sharedTempDir.toString());
    }

    // --- Вспомогательные методы ---

    /**
     * Создаёт тестовый файл в указанном уровне бэкапа
     */
    private void createFile(BackupTier tier, String fileName, String content) throws IOException {
        Path tierPath = sharedTempDir.resolve(tier.name());
        Files.createDirectories(tierPath);
        Path filePath = tierPath.resolve(fileName);
        Files.write(filePath, content.getBytes());
    }

    /**
     * Читает содержимое тестового файла
     */
    private String readFileContent(BackupTier tier, String fileName) throws IOException {
        Path filePath = sharedTempDir.resolve(tier.name()).resolve(fileName);
        return new String(Files.readAllBytes(filePath));
    }

    // --- Тесты для метода list() ---

    @Test
    @DisplayName("Список файлов: каталог уровня не существует — должен вернуть пустой список")
    void list_whenTierDirectoryDoesNotExist_shouldReturnEmptyList() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.DAILY;

        // Act
        List<String> files = fileSystemBackupFileService.list(tier);

        // Assert
        assertTrue(files.isEmpty(), "Должен вернуть пустой список, если каталог уровня не существует");
    }

    @Test
    @DisplayName("Список файлов: каталог уровня пуст — должен вернуть пустой список")
    void list_whenTierDirectoryIsEmpty_shouldReturnEmptyList() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.DAILY;
        Files.createDirectories(sharedTempDir.resolve(tier.name())); // Создаём пустой каталог уровня

        // Act
        List<String> files = fileSystemBackupFileService.list(tier);

        // Assert
        assertTrue(files.isEmpty(), "Должен вернуть пустой список, если каталог уровня пуст");
    }

    @Test
    @DisplayName("Список файлов: должен вернуть все файлы, отсортированные по алфавиту")
    void list_shouldReturnAllFilesSortedAlphabetically() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.DAILY;
        createFile(tier, "file_c.txt", "content c");
        createFile(tier, "file_a.txt", "content a");
        createFile(tier, "file_b.txt", "content b");

        // Act
        List<String> files = fileSystemBackupFileService.list(tier);

        // Assert
        assertEquals(3, files.size(), "Должен вернуть все файлы в уровне бэкапа");
        assertEquals(List.of("file_a.txt", "file_b.txt", "file_c.txt"), files,
                "Файлы должны быть отсортированы по алфавиту");
    }

    @Test
    @DisplayName("Список файлов: должен игнорировать подкаталоги")
    void list_shouldIgnoreSubDirectories() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.DAILY;
        createFile(tier, "regular_file.txt", "content");
        Path subDir = sharedTempDir.resolve(tier.name()).resolve("subdir");
        Files.createDirectories(subDir);
        Files.write(subDir.resolve("another_file.txt"), "nested content".getBytes());

        // Act
        List<String> files = fileSystemBackupFileService.list(tier);

        // Assert
        assertEquals(1, files.size(), "Должен возвращать только обычные файлы, не каталоги");
        assertEquals("regular_file.txt", files.get(0), "Должен вернуть имя обычного файла");
    }

    // --- Тесты для метода load() ---

    @Test
    @DisplayName("Загрузка файла: должен вернуть корректный InputStream")
    void load_shouldReturnCorrectInputStream() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.WEEKLY;
        String fileName = "config.json";
        String content = "{\"setting\": \"value\"}";
        createFile(tier, fileName, content);

        // Act
        try (InputStream inputStream = fileSystemBackupFileService.load(fileName, tier)) {
            byte[] bytes = inputStream.readAllBytes();
            String loadedContent = new String(bytes);

            // Assert
            assertEquals(content, loadedContent, "Загруженное содержимое должно совпадать с оригинальным");
        }
    }

    @Test
    @DisplayName("Загрузка файла: файл не существует — должно выбросить исключение NoSuchFileException")
    void load_whenFileDoesNotExist_shouldThrowFileNotFoundException() {
        // Arrange
        BackupTier tier = BackupTier.WEEKLY;
        String nonExistentFile = "nonexistent.dat";

        // Act & Assert
        assertThrows(NoSuchFileException.class, () -> {
            fileSystemBackupFileService.load(nonExistentFile, tier);
        }, "Загрузка несуществующего файла должна выбрасывать NoSuchFileException");
    }

    // --- Тесты для метода delete() ---


    @Test
    @DisplayName("Удаление файла: должен удалить существующий файл")
    void delete_shouldDeleteExistingFile() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.WEEKLY;
        String fileName = "to_delete.tmp";
        createFile(tier, fileName, "temporary data");
        Path filePath = sharedTempDir.resolve(tier.name()).resolve(fileName);
        assertTrue(Files.exists(filePath), "Файл должен существовать до удаления");

        // Act
        fileSystemBackupFileService.delete(fileName, tier);

        // Assert
        assertFalse(Files.exists(filePath), "Файл должен быть удалён после операции");
    }

    @Test
    @DisplayName("Удаление файла: файл не существует — не должно выбрасывать исключение")
    void delete_whenFileDoesNotExist_shouldNotThrowException() throws IOException {
        // Arrange
        BackupTier tier = BackupTier.WEEKLY;
        String nonExistentFile = "this_file_does_not_exist.bak";

        // Act
        fileSystemBackupFileService.delete(nonExistentFile, tier);

        // Assert (Отсутствие исключений — ожидаемое поведение)
    }

    // --- Тесты для метода getCreationTime() ---


    @Test
    @DisplayName("Время создания файла: должен вернуть корректное время создания")
    void getCreationTime_shouldReturnCorrectCreationTime() throws IOException, InterruptedException {
        // Arrange
        BackupTier tier = BackupTier.MONTHLY;
        String fileName = "timestamped_file.log";
        createFile(tier, fileName, "log entry");

        // Получаем реальный путь к файлу
        Path filePath = sharedTempDir.resolve(tier.name()).resolve(fileName);

        // Act
        FileTime creationTime = fileSystemBackupFileService.getCreationTime(fileName, tier);

        // Assert
        assertNotNull(creationTime, "Время создания не должно быть null");

        // Сравниваем с реальным временем создания файла
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

        FileTime actualCreationTime = attrs.creationTime();

        // Сравниваем полученное время с реальным временем создания
        // Допускаем погрешность в 5 секунд из‑за системных задержек
        long timeDiffMillis = Math.abs(creationTime.toMillis() - actualCreationTime.toMillis());
        assertTrue(timeDiffMillis < 5000,
                "Время создания должно быть близко к реальному времени создания файла (допускается погрешность до 5 секунд)");
    }

    @Test
    @DisplayName("Время создания файла: файл не существует — должно выбросить исключение NoSuchFileException")
    void getCreationTime_whenFileDoesNotExist_shouldThrowFileNotFoundException() {
        // Arrange
        BackupTier tier = BackupTier.MONTHLY;
        String nonExistentFile = "missing_creation_time.data";

        // Act & Assert
        assertThrows(NoSuchFileException.class, () -> {
            fileSystemBackupFileService.getCreationTime(nonExistentFile, tier);
        }, "Получение времени создания для несуществующего файла должно выбрасывать NoSuchFileException");
    }

    // --- Тесты для метода copy() ---

    @Test
    @DisplayName("Копирование файла: должен скопировать файл в другой уровень бэкапа")
    void copy_shouldCopyFileToDifferentTier() throws IOException {
        // Arrange
        BackupTier fromTier = BackupTier.DAILY;
        BackupTier toTier = BackupTier.WEEKLY;
        String fileName = "file_to_copy.txt";
        String content = "data to be copied";

        createFile(fromTier, fileName, content);
        Path srcPath = sharedTempDir.resolve(fromTier.name()).resolve(fileName);
        Path destDirPath = sharedTempDir.resolve(toTier.name());
        Path destPath = destDirPath.resolve(fileName);

        assertTrue(Files.exists(srcPath), "Исходный файл должен существовать до копирования");
        assertFalse(Files.exists(destPath), "Целевой файл не должен существовать до копирования");

        // Act
        fileSystemBackupFileService.copy(fileName, fromTier, toTier);

        // Assert
        assertTrue(Files.exists(srcPath), "Исходный файл должен остаться после копирования");
        assertTrue(Files.exists(destPath), "Целевой файл должен появиться после копирования");
        assertEquals(content, readFileContent(toTier, fileName),
                "Содержимое скопированного файла должно совпадать с оригиналом");
    }

    @Test
    @DisplayName("Копирование файла: целевой уровень не существует — должен создать каталог")
    void copy_whenDestinationTierDoesNotExist_shouldCreateIt() throws IOException {
        // Arrange
        BackupTier fromTier = BackupTier.DAILY;
        BackupTier toTier = BackupTier.MONTHLY; // Этот уровень может ещё не существовать
        String fileName = "file_create_dir.dat";
        String content = "content for new dir";

        createFile(fromTier, fileName, content);
        Path destDirPath = sharedTempDir.resolve(toTier.name());
        assertFalse(Files.exists(destDirPath), "Каталог целевого уровня должен отсутствовать до копирования");

        // Act
        fileSystemBackupFileService.copy(fileName, fromTier, toTier);

        // Assert
        assertTrue(Files.exists(destDirPath), "Каталог целевого уровня должен быть создан после копирования");
        assertTrue(Files.exists(destDirPath.resolve(fileName)),
                "Скопированный файл должен появиться в созданном каталоге");
    }

    @Test
    @DisplayName("Копирование файла: должен перезаписать существующий файл в целевом каталоге")
    void copy_shouldOverwriteExistingFileInDestination() throws IOException {
        // Arrange
        BackupTier fromTier = BackupTier.DAILY;
        BackupTier toTier = BackupTier.WEEKLY;
        String fileName = "file_to_overwrite.txt";
        String originalContent = "original content";
        String newContent = "new content";

        createFile(fromTier, fileName, newContent); // Исходный файл с новым содержимым
        createFile(toTier, fileName, originalContent); // Файл в целевом каталоге со старым содержимым

        Path srcPath = sharedTempDir.resolve(fromTier.name()).resolve(fileName);
        Path destPath = sharedTempDir.resolve(toTier.name()).resolve(fileName);

        // Act
        fileSystemBackupFileService.copy(fileName, fromTier, toTier);

        // Assert
        assertTrue(Files.exists(destPath), "Целевой файл должен существовать после копирования");
        assertEquals(newContent, readFileContent(toTier, fileName),
                "Содержимое целевого файла должно быть перезаписано новым содержимым");
    }

    @Test
    @DisplayName("Копирование файла: исходный файл не существует — должно выбросить исключение NoSuchFileException")
    void copy_whenSourceFileDoesNotExist_shouldThrowFileNotFoundException() {
        // Arrange
        BackupTier fromTier = BackupTier.DAILY;
        BackupTier toTier = BackupTier.WEEKLY;
        String nonExistentFile = "imaginary_file.txt";

        // Act & Assert
        assertThrows(NoSuchFileException.class, () -> {
            fileSystemBackupFileService.copy(nonExistentFile, fromTier, toTier);
        }, "Копирование несуществующего файла должно выбрасывать NoSuchFileException");
    }
}

//package fr.mossaab.security.integration.backup.service.impl;
//
//
//import fr.mossaab.security.backup.core.config.BackupProperties;
//import fr.mossaab.security.backup.core.enums.BackupTier;
//import fr.mossaab.security.backup.core.service.impl.FileSystemBackupFileService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.test.context.TestPropertySource;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.*;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.nio.file.attribute.FileTime;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@TestPropertySource(properties = "backup.file-system.base-path=${java.io.tmpdir}/test-backup")
//class FileSystemBackupFileServiceIT {
//
//    @TempDir
//    Path sharedTempDir;
//
//    @Autowired
//    private FileSystemBackupFileService fileSystemBackupFileService;
//
//    @SpyBean
//    private BackupProperties backupProperties;
//
//    @BeforeEach
//    void setupPath() {
//        backupProperties.getFileSystem().setBasePath(sharedTempDir.toString());
//    }
//
//    // --- Вспомогательные методы для создания тестовых файлов ---
//    private void createFile(BackupTier tier, String fileName, String content) throws IOException {
//        Path tierPath = sharedTempDir.resolve(tier.name());
//        Files.createDirectories(tierPath);
//        Path filePath = tierPath.resolve(fileName);
//        Files.write(filePath, content.getBytes());
//    }
//
//    private String readFileContent(BackupTier tier, String fileName) throws IOException {
//        Path filePath = sharedTempDir.resolve(tier.name()).resolve(fileName);
//        return new String(Files.readAllBytes(filePath));
//    }
//
//    // --- Тесты для метода list() ---
//
//    @Test
//    void list_whenTierDirectoryDoesNotExist_shouldReturnEmptyList() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.DAILY;
//
//        // Act
//        List<String> files = fileSystemBackupFileService.list(tier);
//
//        // Assert
//        assertTrue(files.isEmpty(), "Should return an empty list if the tier directory does not exist.");
//    }
//
//    @Test
//    void list_whenTierDirectoryIsEmpty_shouldReturnEmptyList() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.DAILY;
//        Files.createDirectories(sharedTempDir.resolve(tier.name())); // Создаем пустой каталог уровня
//
//        // Act
//        List<String> files = fileSystemBackupFileService.list(tier);
//
//        // Assert
//        assertTrue(files.isEmpty(), "Should return an empty list if the tier directory is empty.");
//    }
//
//    @Test
//    void list_shouldReturnAllFilesSortedAlphabetically() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.DAILY;
//        createFile(tier, "file_c.txt", "content c");
//        createFile(tier, "file_a.txt", "content a");
//        createFile(tier, "file_b.txt", "content b");
//
//        // Act
//        List<String> files = fileSystemBackupFileService.list(tier);
//
//        // Assert
//        assertEquals(3, files.size(), "Should return all files in the tier.");
//        assertEquals(List.of("file_a.txt", "file_b.txt", "file_c.txt"), files, "Files should be sorted alphabetically.");
//    }
//
//    @Test
//    void list_shouldIgnoreSubDirectories() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.DAILY;
//        createFile(tier, "regular_file.txt", "content");
//        Path subDir = sharedTempDir.resolve(tier.name()).resolve("subdir");
//        Files.createDirectories(subDir);
//        Files.write(subDir.resolve("another_file.txt"), "nested content".getBytes());
//
//        // Act
//        List<String> files = fileSystemBackupFileService.list(tier);
//
//        // Assert
//        assertEquals(1, files.size(), "Should only return regular files, not directories.");
//        assertEquals("regular_file.txt", files.get(0), "Should return the regular file name.");
//    }
//
//    // --- Тесты для метода load() ---
//
//    @Test
//    void load_shouldReturnCorrectInputStream() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.WEEKLY;
//        String fileName = "config.json";
//        String content = "{\"setting\": \"value\"}";
//        createFile(tier, fileName, content);
//
//        // Act
//        try (InputStream inputStream = fileSystemBackupFileService.load(fileName, tier)) {
//            byte[] bytes = inputStream.readAllBytes();
//            String loadedContent = new String(bytes);
//
//            // Assert
//            assertEquals(content, loadedContent, "Loaded content should match the original content.");
//        }
//    }
//
//    @Test
//    void load_whenFileDoesNotExist_shouldThrowFileNotFoundException() {
//        // Arrange
//        BackupTier tier = BackupTier.WEEKLY;
//        String nonExistentFile = "nonexistent.dat";
//
//        // Act & Assert
//        assertThrows(NoSuchFileException.class, () -> {
//            fileSystemBackupFileService.load(nonExistentFile, tier);
//        }, "Loading a non-existent file should throw NoSuchFileException.");
//    }
//
//    // --- Тесты для метода delete() ---
//
//    @Test
//    void delete_shouldDeleteExistingFile() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.WEEKLY;
//        String fileName = "to_delete.tmp";
//        createFile(tier, fileName, "temporary data");
//        Path filePath = sharedTempDir.resolve(tier.name()).resolve(fileName);
//        assertTrue(Files.exists(filePath), "File should exist before deletion.");
//
//        // Act
//        fileSystemBackupFileService.delete(fileName, tier);
//
//        // Assert
//        assertFalse(Files.exists(filePath), "File should be deleted after the operation.");
//    }
//
//    @Test
//    void delete_whenFileDoesNotExist_shouldNotThrowException() throws IOException {
//        // Arrange
//        BackupTier tier = BackupTier.WEEKLY;
//        String nonExistentFile = "this_file_does_not_exist.bak";
//
//        // Act
//        fileSystemBackupFileService.delete(nonExistentFile, tier);
//
//        // Assert (No exception thrown is the expected behavior)
//        // Optionally, check logs if you want to verify debug message, but for integration test,
//        // verifying no exception is usually enough.
//    }
//
//    // --- Тесты для метода getCreationTime() ---
//
//    @Test
//    void getCreationTime_shouldReturnCorrectCreationTime() throws IOException, InterruptedException {
//        // Arrange
//        BackupTier tier = BackupTier.MONTHLY;
//        String fileName = "timestamped_file.log";
//        createFile(tier, fileName, "log entry");
//
//        // Получаем реальный путь к файлу
//        Path filePath = sharedTempDir.resolve(tier.name()).resolve(fileName);
//
//        // Задаем ожидаемое время создания (немного назад от текущего, для надежности)
//        // Операционные системы могут округлять время, поэтому не ожидаем идеального совпадения
//        // Но в данном случае мы создаем файл, поэтому его время создания должно быть близко к реальному.
//        // Для более точного тестирования можно было бы мокировать файловую систему,
//        // но это уже выходит за рамки интеграционного теста.
//
//        // Act
//        FileTime creationTime = fileSystemBackupFileService.getCreationTime(fileName, tier);
//
//        // Assert
//        // Проверяем, что время создания не null и находится в разумных пределах
//        assertNotNull(creationTime, "Creation time should not be null.");
//
//        // Сравним с реальным временем создания файла, если возможно
//        // В идеале, для точного тестирования времени, можно было бы использовать mock,
//        // но для интеграционного теста важно, чтобы оно работало с реальной ФС.
//        // Здесь мы просто проверяем, что оно есть и оно имеет смысл.
//
//        // Можно проверить, что время создания близко к текущему времени создания файла.
//        // Получаем реальные атрибуты файла
//        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
//        FileTime actualCreationTime = attrs.creationTime();
//
//        // Сравниваем полученное время с реальным временем создания.
//        // Может быть небольшая разница из-за системных задержек.
//        long timeDiffMillis = Math.abs(creationTime.toMillis() - actualCreationTime.toMillis());
//        assertTrue(timeDiffMillis < 5000, "Creation time should be close to the actual file creation time."); // Допускаем 5 секунд погрешности
//    }
//
//    @Test
//    void getCreationTime_whenFileDoesNotExist_shouldThrowFileNotFoundException() {
//        // Arrange
//        BackupTier tier = BackupTier.MONTHLY;
//        String nonExistentFile = "missing_creation_time.data";
//
//        // Act & Assert
//        assertThrows(NoSuchFileException.class, () -> {
//            fileSystemBackupFileService.getCreationTime(nonExistentFile, tier);
//        }, "Getting creation time for a non-existent file should throw NoSuchFileException.");
//    }
//
//    // --- Тесты для метода copy() ---
//
//    @Test
//    void copy_shouldCopyFileToDifferentTier() throws IOException {
//        // Arrange
//        BackupTier fromTier = BackupTier.DAILY;
//        BackupTier toTier = BackupTier.WEEKLY;
//        String fileName = "file_to_copy.txt";
//        String content = "data to be copied";
//
//        createFile(fromTier, fileName, content);
//        Path srcPath = sharedTempDir.resolve(fromTier.name()).resolve(fileName);
//        Path destDirPath = sharedTempDir.resolve(toTier.name());
//        Path destPath = destDirPath.resolve(fileName);
//
//        assertTrue(Files.exists(srcPath), "Source file should exist before copy.");
//        assertFalse(Files.exists(destPath), "Destination file should not exist before copy.");
//
//        // Act
//        fileSystemBackupFileService.copy(fileName, fromTier, toTier);
//
//        // Assert
//        assertTrue(Files.exists(srcPath), "Source file should still exist after copy.");
//        assertTrue(Files.exists(destPath), "Destination file should exist after copy.");
//        assertEquals(content, readFileContent(toTier, fileName), "Content of copied file should match.");
//    }
//
//    @Test
//    void copy_whenDestinationTierDoesNotExist_shouldCreateIt() throws IOException {
//        // Arrange
//        BackupTier fromTier = BackupTier.DAILY;
//        BackupTier toTier = BackupTier.MONTHLY; // Этот уровень может еще не существовать
//        String fileName = "file_create_dir.dat";
//        String content = "content for new dir";
//
//        createFile(fromTier, fileName, content);
//        Path destDirPath = sharedTempDir.resolve(toTier.name());
//        assertFalse(Files.exists(destDirPath), "Destination tier directory should not exist before copy.");
//
//        // Act
//        fileSystemBackupFileService.copy(fileName, fromTier, toTier);
//
//        // Assert
//        assertTrue(Files.exists(destDirPath), "Destination tier directory should be created after copy.");
//        assertTrue(Files.exists(destDirPath.resolve(fileName)), "Copied file should exist in the created directory.");
//    }
//
//    @Test
//    void copy_shouldOverwriteExistingFileInDestination() throws IOException {
//        // Arrange
//        BackupTier fromTier = BackupTier.DAILY;
//        BackupTier toTier = BackupTier.WEEKLY;
//        String fileName = "file_to_overwrite.txt";
//        String originalContent = "original content";
//        String newContent = "new content";
//
//        createFile(fromTier, fileName, newContent); // Создаем исходный файл с новым содержимым
//        createFile(toTier, fileName, originalContent); // Создаем файл в целевом каталоге со старым содержимым
//
//        Path srcPath = sharedTempDir.resolve(fromTier.name()).resolve(fileName);
//        Path destPath = sharedTempDir.resolve(toTier.name()).resolve(fileName);
//
//        // Act
//        fileSystemBackupFileService.copy(fileName, fromTier, toTier);
//
//        // Assert
//        assertTrue(Files.exists(destPath), "Destination file should exist after copy.");
//        assertEquals(newContent, readFileContent(toTier, fileName), "Destination file content should be overwritten.");
//    }
//
//    @Test
//    void copy_whenSourceFileDoesNotExist_shouldThrowFileNotFoundException() {
//        // Arrange
//        BackupTier fromTier = BackupTier.DAILY;
//        BackupTier toTier = BackupTier.WEEKLY;
//        String nonExistentFile = "imaginary_file.txt";
//
//        // Act & Assert
//        assertThrows(NoSuchFileException.class, () -> {
//            fileSystemBackupFileService.copy(nonExistentFile, fromTier, toTier);
//        }, "Copying a non-existent file should throw NoSuchFileException.");
//    }
//}