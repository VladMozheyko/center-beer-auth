package fr.mossaab.security.backup.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.backup.core.report.BackupReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Утилита для чтения данных из ZIP-архива бэкапа.
 * <p>
 * Позволяет:
 * <ul>
 *     <li>десериализовать JSON-объекты из файлов внутри ZIP;</li>
 *     <li>прочитать произвольный entry в массив байт;</li>
 *     <li>проверить по report.json, был ли бэкап успешным.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupArchiveReader {

    private final ObjectMapper objectMapper;

    /**
     * Читает и десериализует указанный JSON-файл внутри ZIP-архива в объект заданного типа.
     *
     * @param zipStream поток с ZIP-архивом (вызывающий обязан его закрыть)
     * @param entryName имя файла внутри архива (например, "backup.json")
     * @param type      класс целевого типа
     * @param <T>       тип результата
     * @return десериализованный объект или {@code null}, если entry не найден
     * @throws IOException при ошибке чтения или десериализации
     */
    public <T> T readFromZip(InputStream zipStream, String entryName, Class<T> type) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(zipStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    log.debug("[BACKUP_ARCHIVE_READER] Найден entry '{}' в ZIP, начинаю десериализацию в {}",
                            entryName, type.getSimpleName());
                    return objectMapper.readValue(zipIn, type);
                }
            }
        }
        log.error("[BACKUP_ARCHIVE_READER] Entry '{}' не найден в ZIP-архиве", entryName);
        return null;
    }

    /**
     * Читает содержимое указанного файла внутри ZIP-архива в массив байт.
     *
     * @param zipInputStream входной поток ZIP (например, от backupStorage.load(...));
     *                       вызывающий обязан его закрыть
     * @param entryName      имя файла внутри архива (например, "backup.json")
     * @return содержимое файла в виде byte[]
     * @throws IOException если файл не найден в архиве или произошла ошибка чтения
     */
    public byte[] readBackupBytes(InputStream zipInputStream, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entryName.equals(entry.getName())) {
                    log.debug("[BACKUP_ARCHIVE_READER] Чтение entry '{}' в память", entryName);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    zis.closeEntry();
                    return baos.toByteArray();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("[BACKUP_ARCHIVE_READER] Ошибка при чтении '{}' из ZIP-архива", entryName, e);
            throw e;
        }

        // если дошли сюда – нужного файла в архиве нет
        String msg = "Entry '" + entryName + "' not found in ZIP archive";
        log.error("[BACKUP_ARCHIVE_READER] {}", msg);
        throw new IOException(msg);
    }

    /**
     * Проверяет, является ли бэкап в переданном ZIP-потоке успешным,
     * основываясь на данных из report.json.
     *
     * @param zipStream поток с ZIP-архивом бэкапа (вызывающий обязан его закрыть)
     * @return {@code true}, если отчёт помечен как успешный,
     *         {@code false}, если отчёта нет, он не найден или при чтении произошла ошибка
     */
    public boolean isBackupSuccessful(InputStream zipStream) {
        try {
            BackupReport report = readReport(zipStream);
            if (report == null) {
                log.warn("[BACKUP_ARCHIVE_READER] report.json не найден или не удалось прочитать отчёт");
                return false;
            }
            boolean success = "SUCCESS".equalsIgnoreCase(report.getStatus().name());
            log.debug("[BACKUP_ARCHIVE_READER] Статус бэкапа из report.json: {}", report.getStatus());
            return success;
        } catch (IOException e) {
            log.error("[BACKUP_ARCHIVE_READER] Ошибка при чтении report.json из ZIP-архива", e);
            return false;
        }
    }

    /**
     * Читает report.json из ZIP-архива и десериализует в {@link BackupReport}.
     *
     * @param zipStream поток с ZIP-архивом бэкапа (вызывающий обязан его закрыть)
     * @return объект {@link BackupReport} или {@code null}, если report.json не найден
     * @throws IOException при ошибке чтения или десериализации
     */
    public BackupReport readReport(InputStream zipStream) throws IOException {
        return readFromZip(zipStream, "report.json", BackupReport.class);
    }
}