package fr.mossaab.security.unit.backup.service;

import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.impl.FileSystemBackupFileService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UnitTests - FileSystemBackupFileService")
class FileSystemBackupFileServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemBackupFileService service;

    @BeforeEach
    void setUp() {
        BackupProperties.FileSystemStorageProperties fsProps = new BackupProperties.FileSystemStorageProperties();
        fsProps.setBasePath(tempDir.toString());

        BackupProperties props = new BackupProperties();
        props.setFileSystem(fsProps);

        service = new FileSystemBackupFileService(props);
    }

    @Test
    @DisplayName("list: возвращает пустой список, если каталог tier не существует")
    void list_shouldReturnEmptyListWhenTierDirDoesNotExist() throws Exception {
        Path tierDir = tempDir.resolve(BackupTier.DAILY.name());
        assertThat(Files.exists(tierDir)).isFalse();

        List<String> files = service.list(BackupTier.DAILY);

        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("list: возвращает список только обычных файлов, отсортированный по имени")
    void list_shouldReturnSortedRegularFiles() throws Exception {
        Path tierDir = tempDir.resolve(BackupTier.DAILY.name());
        Files.createDirectories(tierDir);

        Files.createFile(tierDir.resolve("b2.zip"));
        Files.createFile(tierDir.resolve("b1.zip"));
        Files.createDirectory(tierDir.resolve("subdir")); // должен быть проигнорирован

        List<String> files = service.list(BackupTier.DAILY);

        assertThat(files).containsExactly("b1.zip", "b2.zip");
    }

    @Test
    @DisplayName("load: возвращает InputStream, если файл существует")
    void load_shouldReturnInputStreamWhenFileExists() throws Exception {
        Path tierDir = tempDir.resolve(BackupTier.DAILY.name());
        Files.createDirectories(tierDir);

        Path file = tierDir.resolve("backup.zip");
        Files.writeString(file, "test-data");

        try (InputStream in = service.load("backup.zip", BackupTier.DAILY)) {
            byte[] bytes = in.readAllBytes();
            assertThat(new String(bytes)).isEqualTo("test-data");
        }
    }

    @Test
    @DisplayName("load: бросает NoSuchFileException, если файл не существует")
    void load_shouldThrowWhenFileDoesNotExist() {
        assertThatThrownBy(() -> service.load("missing.zip", BackupTier.DAILY))
                .isInstanceOf(NoSuchFileException.class);
    }

    @Test
    @DisplayName("delete: удаляет существующий файл и не кидает исключение")
    void delete_shouldDeleteExistingFile() throws Exception {
        Path tierDir = tempDir.resolve(BackupTier.DAILY.name());
        Files.createDirectories(tierDir);

        Path file = tierDir.resolve("backup.zip");
        Files.createFile(file);

        assertThat(Files.exists(file)).isTrue();

        service.delete("backup.zip", BackupTier.DAILY);

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    @DisplayName("delete: ничего не делает, если файла нет (без исключения)")
    void delete_shouldDoNothingWhenFileDoesNotExist() throws Exception {
        Path tierDir = tempDir.resolve(BackupTier.DAILY.name());
        Files.createDirectories(tierDir);

        Path file = tierDir.resolve("missing.zip");
        assertThat(Files.exists(file)).isFalse();

        service.delete("missing.zip", BackupTier.DAILY);

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    @DisplayName("getCreationTime: возвращает время создания файла")
    void getCreationTime_shouldReturnFileCreationTime() throws Exception {
        Path tierDir = tempDir.resolve(BackupTier.DAILY.name());
        Files.createDirectories(tierDir);

        Path file = tierDir.resolve("backup.zip");
        Files.createFile(file);

        FileTime expected = FileTime.from(Instant.now().minusSeconds(60));
        Files.setAttribute(file, "basic:creationTime", expected);

        FileTime creationTime = service.getCreationTime("backup.zip", BackupTier.DAILY);

        assertThat(creationTime.toMillis()).isEqualTo(expected.toMillis());
    }

    @Test
    @DisplayName("getCreationTime: бросает NoSuchFileException, если файла нет")
    void getCreationTime_shouldThrowWhenFileDoesNotExist() {
        assertThatThrownBy(() -> service.getCreationTime("missing.zip", BackupTier.DAILY))
                .isInstanceOf(IOException.class); // конкретно обычно NoSuchFileException
    }

    @Test
    @DisplayName("copy: копирует файл из одного tier в другой, создавая директорию назначения")
    void copy_shouldCopyFileBetweenTiers() throws Exception {
        Path fromDir = tempDir.resolve(BackupTier.DAILY.name());
        Files.createDirectories(fromDir);

        Path src = fromDir.resolve("backup.zip");
        Files.writeString(src, "content");

        Path toDir = tempDir.resolve(BackupTier.WEEKLY.name());
        assertThat(Files.exists(toDir)).isFalse();

        service.copy("backup.zip", BackupTier.DAILY, BackupTier.WEEKLY);

        Path dest = toDir.resolve("backup.zip");
        assertThat(Files.exists(dest)).isTrue();
        assertThat(Files.readString(dest)).isEqualTo("content");
    }

    @Test
    @DisplayName("copy: бросает NoSuchFileException, если исходный файл не существует")
    void copy_shouldThrowWhenSourceDoesNotExist() {
        assertThatThrownBy(() -> service.copy("missing.zip", BackupTier.DAILY, BackupTier.WEEKLY))
                .isInstanceOf(NoSuchFileException.class);
    }
}