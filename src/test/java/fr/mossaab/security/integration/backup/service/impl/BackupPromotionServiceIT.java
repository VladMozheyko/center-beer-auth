package fr.mossaab.security.integration.backup.service.impl;

import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupFileExtension;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.impl.BackupPromotionService;
import fr.mossaab.security.backup.core.utils.BackupFileNameGenerator;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;


@SpringBootTest
@DisplayName("IntegrationTest - BackupPromotionService")
class BackupPromotionServiceIT extends AbstractIntegrationTest {

    @Autowired
    private BackupFileNameGenerator fileNameGenerator;

    @Autowired
    private BackupProperties properties;

    @SpyBean
    private BackupPromotionService promotionService;

    private Path basePath;

    @BeforeEach
    public void setUp() throws IOException {
        basePath = Path.of(properties.getFileSystem().getBasePath());
        // на всякий случай очищаем и DAILY, и WEEKLY перед каждым тестом
        clearDirectory(basePath.resolve(BackupTier.DAILY.name()));
        clearDirectory(basePath.resolve(BackupTier.WEEKLY.name()));
    }

    @AfterEach
    public void tearDown() throws IOException {
        clearDirectory(basePath.resolve(BackupTier.DAILY.name()));
        clearDirectory(basePath.resolve(BackupTier.WEEKLY.name()));
    }

    @Test
    @DisplayName("Промоутит самый новый успешный DAILY-бэкап в WEEKLY в пределах периода")
    void promote_ShouldCopyMostRecentSuccessfulDailyBackupToWeeklyWithinPeriod() throws IOException {
        Instant now1 = Instant.now();

        // создаём первый файл (будет более старым по времени создания)
        String fOld = createFileWithDate(now1);

        try {
            Thread.sleep(100); // небольшая пауза, чтобы creationTime отличался
        } catch (InterruptedException e) {
            throw new RuntimeException("Ошибка задержки времени при тесте", e);
        }

        Instant now2 = Instant.now();
        String fNew = createFileWithDate(now2);

        // убедимся, что оба файла лежат в DAILY
        checkFiles(List.of(fNew, fOld), BackupTier.DAILY);

        // мокаем isSuccessful: оба файла считаем успешными
        doReturn(true).when(promotionService)
                .isSuccessful(any(), eq(BackupTier.DAILY));

        // period = 1 день → оба файла попадают в окно, но fNew — самый новый по creationTime
        promotionService.promote(BackupTier.DAILY, BackupTier.WEEKLY, Period.ofDays(1));

        // в DAILY оба файла остаются
        checkFiles(List.of(fNew, fOld), BackupTier.DAILY);

        // в WEEKLY должен появиться только самый новый — fNew
        checkFiles(List.of(fNew), BackupTier.WEEKLY);
    }

    // ---------- служебные методы ниже ----------

    private void clearDirectory(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(dir))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + path, e);
                        }
                    });
        }
    }

    private void checkFiles(List<String> expectedFileNames, BackupTier tier) throws IOException {
        Path dir = basePath.resolve(tier.name());

        if (!Files.exists(dir)) {
            assertThat(expectedFileNames)
                    .as("Directory %s does not exist, but some files are expected", dir)
                    .isEmpty();
            return;
        }

        try (Stream<Path> stream = Files.list(dir)) {
            List<String> actualNames = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .toList();

            assertThat(actualNames)
                    .as("Files in directory %s", dir)
                    .containsExactlyInAnyOrderElementsOf(expectedFileNames);
        }
    }

    private String createFileWithDate(Instant instant) throws IOException {
        Path absPath = basePath.resolve(BackupTier.DAILY.name());

        Files.createDirectories(absPath);

        String fileName = fileNameGenerator.generate(instant, "1", BackupFileExtension.ZIP);
        Files.createFile(absPath.resolve(fileName));

        return fileName;
    }
}