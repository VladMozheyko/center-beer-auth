package fr.mossaab.security.backup.core.storage;

import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;

/**
 * Реализация {@link BackupStorage} по умолчанию, использующая локальную файловую систему.
 * <p>
 * Бэкапы сохраняются в каталог, заданный в {@link BackupProperties},
 * с разбиением по подкаталогам по уровню хранения ({@link BackupTier}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemBackupStorage implements BackupStorage {

    private final BackupProperties properties;

    /**
     * Определяет базовый каталог для заданного уровня хранения.
     */
    private Path resolveBaseDir(BackupTier tier) {
        Path basePath = Path.of(properties.getFileSystem().getBasePath());
        return (tier != null) ? basePath.resolve(tier.name()) : basePath;
    }

    /**
     * Определяет полный путь к файлу бэкапа.
     */
    private Path resolveFile(BackupTier tier, String backupName) {
        return resolveBaseDir(tier).resolve(backupName);
    }

    @Override
    public void save(BackupTier tier, String backupName, InputStream content) throws IOException {
        Path targetDirectory = resolveBaseDir(tier);
        Files.createDirectories(targetDirectory);

        Path backupFile = targetDirectory.resolve(backupName);

        try (OutputStream out = Files.newOutputStream(backupFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = content.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            log.info("[BACKUP_STORAGE] Резервная копия сохранена: уровень={} файл={}", tier, backupFile);
        } catch (IOException e) {
            log.error("[BACKUP_STORAGE] Ошибка при сохранении резервной копии: уровень={} файл={}",
                    tier, backupFile, e);
            throw e;
        } finally {
            try {
                content.close();
            } catch (IOException e) {
                log.warn("[BACKUP_STORAGE] Ошибка при закрытии входного потока: уровень={} файл={}",
                        tier, backupFile, e);
            }
        }
    }

    @Override
    public InputStream load(String backupName, BackupTier tier) throws IOException {
        Path backupFile = resolveFile(tier, backupName);

        if (!Files.exists(backupFile)) {
            throw new NoSuchFileException("Backup file not found: " + backupFile.toAbsolutePath());
        }

        log.info("[BACKUP_STORAGE] Загрузка резервной копии: уровень={} файл={}", tier, backupFile);
        return Files.newInputStream(backupFile, StandardOpenOption.READ);
    }

    @Override
    public void delete(BackupTier tier, String backupName) throws IOException {
        Path backupFile = resolveFile(tier, backupName);
        Files.deleteIfExists(backupFile);
        log.info("[BACKUP_STORAGE] Удалена резервная копия: уровень={} файл={}", tier, backupFile);
    }
}