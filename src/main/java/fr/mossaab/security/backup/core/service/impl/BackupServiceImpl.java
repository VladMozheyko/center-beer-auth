package fr.mossaab.security.backup.core.service.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.*;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.report.BackupReportCounter;
import fr.mossaab.security.backup.core.report.BackupReportEntry;
import fr.mossaab.security.backup.core.report.BackupSummary;
import fr.mossaab.security.backup.core.service.BackupService;
import fr.mossaab.security.backup.core.service.BackupVersionHandler;
import fr.mossaab.security.backup.core.storage.BackupStorage;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import fr.mossaab.security.backup.core.utils.BackupFileNameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Базовая реализация {@link BackupService}.
 * <p>
 * Отвечает за экспорт и восстановление системного бэкапа через версионные хендлеры.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class BackupServiceImpl implements BackupService {

    private final BackupProperties backupProperties;
    private final BackupStorage backupStorage;
    private final BackupFileNameGenerator fileNameGenerator;
    private final BackupArchiveReader archiveReader;
    private final ObjectMapper objectMapper;
    private final PreRestoreCleaner cleaner;

    /**
     * Обработчики разных версий схемы бэкапа.
     */
    private final List<BackupVersionHandler> versionHandlers;

    private static final String EXPORT_LOG_PREFIX = "[BACKUP_EXPORT] [backupName={}] - ";
    private static final String RESTORE_LOG_PREFIX = "[BACKUP_RESTORE] [backupName={}] - ";

    /**
     * Возвращает хендлер для указанной версии схемы бэкапа.
     *
     * @param version номер версии схемы
     * @return соответствующий {@link BackupVersionHandler}
     * @throws IllegalStateException если хендлер для такой версии не зарегистрирован
     */
    private BackupVersionHandler getHandlerForVersion(int version) {
        return versionHandlers.stream()
                .filter(h -> h.getSchemaVersion() == version)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No BackupVersionHandler registered for schemaVersion=" + version));
    }

    @Override
    @Transactional(readOnly = true)
    public BackupReport exportSystemBackup() {
        Instant startedAt = Instant.now();
        int schemaVersion = backupProperties.getCurrentSchemaVersion();

        String backupFileName = fileNameGenerator.generate(
                startedAt,
                String.valueOf(schemaVersion),
                BackupFileExtension.ZIP
        );
        String shortName = fileNameGenerator.toShortName(backupFileName);

        log.info(EXPORT_LOG_PREFIX + "начало создания бэкапа (schemaVersion={}, sourceName={})",
                shortName, schemaVersion, backupFileName);

        BackupReport report = new BackupReport();
        report.setFileName(backupFileName);
        report.setOperation(BackupOperationType.EXPORT);
        report.setSchemaVersion(schemaVersion);
        report.setStartedAt(startedAt);

        BackupSummary summary = new BackupSummary();
        report.setSummary(summary);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("backup-", ".zip");
            log.debug(EXPORT_LOG_PREFIX + "создан временный файл {}", shortName, tempFile);

            BackupVersionHandler handler = getHandlerForVersion(schemaVersion);
            log.debug(EXPORT_LOG_PREFIX + "используется handler схемы версии {}", shortName, schemaVersion);

            try (OutputStream fileOut = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING);
                 ZipOutputStream zipOut = new ZipOutputStream(fileOut, StandardCharsets.UTF_8)) {

                // 1) backup.json
                zipOut.putNextEntry(new ZipEntry("backup.json"));
                JsonGenerator generator = objectMapper.getFactory().createGenerator(zipOut);

                generator.writeStartObject();
                generator.writeStringField("archiveFileName", backupFileName);
                generator.writeNumberField("schemaVersion", schemaVersion);
                generator.writeStringField("exportedAt", Instant.now().toString());
                generator.writeStringField("appVersion", backupProperties.getAppVersion());

                // делегируем запись данных конкретной версии
                handler.exportData(generator, report);

                generator.writeEndObject();
                generator.flush();

                if (summary.getSkipped() == 0 && summary.getErrors() == 0) {
                    report.setStatus(BackupOperationStatus.SUCCESS);
                } else {
                    report.setStatus(BackupOperationStatus.COMPLETED_WITH_WARNINGS);
                }
                report.setFinishedAt(Instant.now());

                // 2) report.json
                zipOut.putNextEntry(new ZipEntry("report.json"));
                if (backupProperties.isFormattedJson()) {
                    objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValue(zipOut, report);
                } else {
                    objectMapper.writeValue(zipOut, report);
                }
            }

            try (InputStream in = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                backupStorage.save(BackupTier.DAILY, backupFileName, in);
            }

            log.info(EXPORT_LOG_PREFIX + "успешно сохранён ZIP в хранилище: tier={}",
                    shortName, BackupTier.DAILY);
            return report;

        } catch (Exception e) {
            log.error(EXPORT_LOG_PREFIX + "ошибка при создании резервной копии: tier={}, fileName={}",
                    shortName, BackupTier.DAILY, backupFileName, e);
            return createErrorReport(e, report, backupFileName);

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug(EXPORT_LOG_PREFIX + "временный файл удалён: {}", shortName, tempFile);
                } catch (IOException e) {
                    log.warn(EXPORT_LOG_PREFIX + "не удалось удалить временный файл={}",
                            shortName, tempFile, e);
                }
            }
        }
    }

    @Override
    @Transactional
    public BackupReport restoreSystemBackup(String backupName, BackupTier tier) {
        //Очистка базы данных перед восстановлением
        cleaner.cleanAllEntities();

        Instant startedAt = Instant.now();
        String shortName = fileNameGenerator.toShortName(backupName);

        log.info(RESTORE_LOG_PREFIX + "начало восстановления из tier={}", shortName, tier);

        BackupReport report = new BackupReport();
        report.setOperation(BackupOperationType.RESTORE);
        report.setStartedAt(startedAt);
        report.setFileName(backupName);

        BackupSummary summary = new BackupSummary();
        List<BackupReportCounter> details = new ArrayList<>();
        report.setSummary(summary);
        report.setDetails(details);

        try (InputStream fileIn = backupStorage.load(backupName, tier)) {
            log.debug(RESTORE_LOG_PREFIX + "файл бэкапа считан из хранилища (tier={})",
                    shortName, tier);

            // 1. достаём backup.json как байты из ZIP
            byte[] backupJsonBytes = archiveReader.readBackupBytes(fileIn, "backup.json");
            log.debug(RESTORE_LOG_PREFIX + "backup.json прочитан из архива", shortName);

            // 2. читаем schemaVersion
            var rootNode = objectMapper.readTree(backupJsonBytes);
            int schemaVersion = rootNode.path("schemaVersion").asInt(1);
            report.setSchemaVersion(schemaVersion);
            log.debug(RESTORE_LOG_PREFIX + "определена версия схемы: {}",
                    shortName, schemaVersion);

            BackupVersionHandler handler = getHandlerForVersion(schemaVersion);
            log.debug(RESTORE_LOG_PREFIX + "используется handler схемы версии {}",
                    shortName, schemaVersion);

            // 3. делегируем импорт конкретной версии
            try (InputStream jsonIn = new ByteArrayInputStream(backupJsonBytes)) {
                BackupReport versionReport = handler.importData(jsonIn);

                // переносим нужные поля
                report.setStatus(versionReport.getStatus());
                report.setSummary(versionReport.getSummary());
                report.setDetails(versionReport.getDetails());
            }

            report.setFinishedAt(Instant.now());
            log.info(RESTORE_LOG_PREFIX + "восстановление завершено со статусом {}",
                    shortName, report.getStatus());
            return report;

        } catch (NoSuchFileException e) {
            log.error(RESTORE_LOG_PREFIX + "файл бэкапа не найден (tier={})",
                    shortName, tier, e);

            createErrorReport(e, report, backupName);
            return report;

        } catch (IOException e) {
            log.error(RESTORE_LOG_PREFIX + "ошибка чтения или разбора бэкапа (tier={})",
                    shortName, tier, e);

           createErrorReport(e, report, backupName);
            return report;
        }
    }

    /**
     * Формирует отчёт об ошибке создания бэкапа/восстановления.
     *
     * @param e         исключение, вызвавшее ошибку
     * @param report    отчёт, который нужно дополнить
     * @param fullName  имя бэкапа для логов
     * @return отчёт со статусом FAILED и заполненной сводкой ошибок
     */
    private BackupReport createErrorReport(Exception e, BackupReport report, String fullName) {
        BackupSummary summary = BackupSummary.builder()
                .errors(1)
                .build();
        BackupReportEntry entry = BackupReportEntry.builder()
                .reasonMessage(e.getMessage())
                .build();

        BackupReportCounter details = new BackupReportCounter();
        details.setEntityName(fullName);
        details.setDetails(List.of(entry));

        report.setDetails(List.of(details));
        report.setStatus(BackupOperationStatus.FAILED);
        report.setSummary(summary);
        report.setFinishedAt(Instant.now());
        return report;
    }
}