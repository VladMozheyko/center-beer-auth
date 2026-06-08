package fr.mossaab.security.backup.core.utils;

import fr.mossaab.security.backup.core.enums.BackupFileExtension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BackupFileNameGenerator {

    private final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss")
            .withZone(ZoneId.of("Europe/Moscow"));

    /**
     * Генерирует имя файла резервной копии.
     * <p>
     * Формат имени включает:
     * <ul>
     *     <li>Версию приложения в виде {@code vX_Y_Z}.</li>
     *     <li>Момент начала операции в UTC в формате {@code yyyyMMdd'T'HHmmss'Z'}.</li>
     *     <li>Случайный {@link UUID} для уникальности.</li>
     *     <li>Расширение, заданное {@link BackupFileExtension}.</li>
     * </ul>
     *
     * Примеры:
     * <pre>
     * backup-v1_2_3-20250520T143015Z-3f1a9b2c-... .zip
     * </pre>
     *
     * @param startedAt момент начала операции резервного копирования
     * @param ext       расширение файла бэкапа
     * @return сгенерированное имя файла резервной копии
     */
    public String generate(Instant startedAt, String appVersion, BackupFileExtension ext) {
        String ts = formatter.format(startedAt);
        String uuid = UUID.randomUUID().toString();
        String version = appVersion.replace(".", "_");
        return "backup-v" + version + "-" + ts + "-" + uuid + ext.getExtension();
    }

    /**
     * Возвращает укороченную версию имени файла для использования в логах.
     * <p>
     * Извлекает расширение из полного имени, удаляет его, возвращает базовое имя.
     * Поддерживает расширения любой длины (json, json.gz, zip и т. д.).
     *
     * @param fullName полное имя файла резервной копии
     * @return сокращённое имя файла для логирования (без расширения)
     */
    public String toShortName(String fullName) {
        int lastDotIndex = fullName.lastIndexOf('.');
        if (lastDotIndex > 27) {
            return fullName.substring(27, lastDotIndex);
        }
        return fullName;
    }
}
