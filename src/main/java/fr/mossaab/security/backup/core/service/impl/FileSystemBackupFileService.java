package fr.mossaab.security.backup.core.service.impl;

import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Реализация {@link BackupFileService} на основе локальной файловой системы.
 * <p>
 * Структура каталогов:
 * <pre>
 *   {basePath}/
 *     ├── DAILY/
 *     ├── WEEKLY/
 *     ├── MONTHLY/
 *     └── ...
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemBackupFileService implements BackupFileService {

    private final BackupProperties properties;

    /**
     * Возвращает путь к каталогу указанного уровня хранения.
     *
     * @param tier уровень хранения
     * @return путь к каталогу уровня
     */
    private Path resolveTierDir(BackupTier tier) {
        return Path.of(properties.getFileSystem().getBasePath())
                .resolve(tier.name());
    }

    /**
     * Возвращает путь к файлу резервной копии в указанном уровне хранения.
     *
     * @param tier     уровень хранения
     * @param fileName имя файла
     * @return полный путь к файлу
     */
    private Path resolveFile(BackupTier tier, String fileName) {
        return resolveTierDir(tier).resolve(fileName);
    }

    @Override
    public List<String> list(BackupTier tier) throws IOException {
        Path path = resolveTierDir(tier);
        if (!Files.exists(path)) {
            log.debug("[BACKUP_FS] Каталог уровня {} не существует, возвращаем пустой список ({})", tier, path);
            return List.of();
        }
        try (Stream<Path> stream = Files.list(path)) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
            log.debug("[BACKUP_FS] Найдено {} файлов в уровне {} ({})", files.size(), tier, path);
            return files;
        }
    }

    @Override
    public InputStream load(String fileName, BackupTier tier) throws IOException {
        Path path = resolveFile(tier, fileName);
        log.debug("[BACKUP_FS] Чтение файла бэкапа: tier={} file={} path={}", tier, fileName, path);
        return Files.newInputStream(path);
    }

    @Override
    public void delete(String fileName, BackupTier tier) throws IOException {
        Path path = resolveFile(tier, fileName);
        boolean deleted = Files.deleteIfExists(path);
        if (deleted) {
            log.info("[BACKUP_FS] Удалён файл бэкапа: tier={} file={} path={}", tier, fileName, path);
        } else {
            log.debug("[BACKUP_FS] Файл для удаления не найден: tier={} file={} path={}", tier, fileName, path);
        }
    }

    @Override
    public FileTime getCreationTime(String fileName, BackupTier tier) throws IOException {
        Path path = resolveFile(tier, fileName);
        FileTime creationTime = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
        log.trace("[BACKUP_FS] Время создания файла: tier={} file={} createdAt={}", tier, fileName, creationTime);
        return creationTime;
    }

    @Override
    public void copy(String fileName, BackupTier fromTier, BackupTier toTier) throws IOException {
        Path src = resolveFile(fromTier, fileName);
        Path destDir = resolveTierDir(toTier);
        Files.createDirectories(destDir);
        Path dest = destDir.resolve(fileName);

        // Можно copy, а можно через BackupStorage.save, если хочешь унифицировать
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("[BACKUP_FS] Скопирован файл бэкапа: {} -> {} (file={})", fromTier, toTier, fileName);
    }
}