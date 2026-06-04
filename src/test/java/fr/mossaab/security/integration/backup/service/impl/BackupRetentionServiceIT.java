package fr.mossaab.security.integration.backup.service.impl;

import fr.mossaab.security.SecurityApplication;
import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.config.BackupRetentionProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import fr.mossaab.security.backup.core.service.impl.BackupRetentionService;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SecurityApplication.class)
@DisplayName("IntegrationTest - BackupRetentionService")
class BackupRetentionServiceIT extends AbstractIntegrationTest {

    private static Path tempBaseDir;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) throws IOException {
        tempBaseDir = Files.createTempDirectory("backup-it-");
        // Подстраиваемся под структуру BackupProperties
        // Если у тебя префикс другой – поправь здесь
        registry.add("backup.file-system.base-path", () -> tempBaseDir.toString());
    }

    @Autowired
    BackupRetentionService backupRetentionService;

    @Autowired
    BackupFileService backupFileService;

    @Autowired
    BackupRetentionProperties backupRetentionProperties;

    @Autowired
    BackupProperties backupProperties;

    @BeforeEach
    void setupRetentionConfig() {
        // Настраиваем уже существующий бин Retention
        backupRetentionProperties.setDailyDays(3);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (tempBaseDir != null && Files.exists(tempBaseDir)) {
            try (var paths = Files.walk(tempBaseDir)) {
                paths.sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
            }
        }
    }

    @Test
    @DisplayName("Интеграционно: BackupRetentionService удаляет старые DAILY бэкапы через FileSystemBackupFileService")
    void applyRetention_Daily_RemovesOldFiles() throws Exception {
        BackupTier tier = BackupTier.DAILY;

        // Проверим, что файловый сервис действительно смотрит в наш tempBaseDir
        assertThat(backupProperties.getFileSystem().getBasePath())
                .isEqualTo(tempBaseDir.toString());

        // Каталог для данного уровня: {basePath}/DAILY
        Path tierDir = tempBaseDir.resolve(tier.name());
        Files.createDirectories(tierDir);

        // Расчёт cutoff такой же, как в BackupRetentionService
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate cutoffDate = now.minusDays(backupRetentionProperties.getDailyDays());
        Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        // 1. Старый файл: сильно старше cutoff -> должен быть удалён
        Path oldFile = tierDir.resolve("daily-old.zip");
        Files.writeString(oldFile, "old-backup");
        Instant oldCreated = cutoffInstant.minusSeconds(5L * 24 * 3600);
        Files.setAttribute(oldFile, "basic:creationTime", FileTime.from(oldCreated));

        // 2. Новый файл: моложе cutoff -> должен остаться
        Path newFile = tierDir.resolve("daily-new.zip");
        Files.writeString(newFile, "new-backup");
        Instant newCreated = cutoffInstant.plusSeconds(24 * 3600);
        Files.setAttribute(newFile, "basic:creationTime", FileTime.from(newCreated));

        // sanity check
        assertThat(Files.exists(oldFile)).isTrue();
        assertThat(Files.exists(newFile)).isTrue();

        // Действие
        backupRetentionService.applyRetention(tier);

        // Проверяем результат на файловой системе
        assertThat(Files.exists(oldFile)).isFalse();
        assertThat(Files.exists(newFile)).isTrue();

        // И через сам FileSystemBackupFileService
        assertThat(backupFileService.list(tier))
                .containsExactly("daily-new.zip");
    }
}