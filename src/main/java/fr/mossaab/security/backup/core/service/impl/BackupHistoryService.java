package fr.mossaab.security.backup.core.service.impl;

import fr.mossaab.security.backup.core.dto.response.BackupReportSummaryResponse;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.report.BackupSummary;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Сервис просмотра истории резервных копий.
 * <p>
 * Читает ZIP‑архивы через {@link BackupFileService}, извлекает из них {@code report.json}
 * и на основе него возвращает:
 * <ul>
 *     <li>список кратких отчётов по всем бэкапам ({@link BackupReportSummaryResponse});</li>
 *     <li>полный отчёт по конкретному бэкапу ({@link BackupReport}).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupHistoryService {

    private final BackupFileService backupFileService;
    private final BackupArchiveReader archiveReader;

    /**
     * Возвращает список кратких отчётов по бэкапам с учётом фильтров.
     *
     * @param tierFilter   уровень хранения; если {@code null}, берутся все уровни
     * @param statusFilter статус операции; если {@code null}, берутся любые статусы
     * @return список сводных отчётов, отсортированный по времени старта (по убыванию)
     */
    public List<BackupReportSummaryResponse> listHistory(
            BackupTier tierFilter,
            BackupOperationStatus statusFilter
    ) {
        List<BackupReportSummaryResponse> result = new ArrayList<>();

        List<BackupTier> tiers = (tierFilter != null)
                ? List.of(tierFilter)
                : List.of(BackupTier.values());

        for (BackupTier tier : tiers) {
            List<String> files;
            try {
                files = backupFileService.list(tier);
            } catch (IOException e) {
                log.warn("[BACKUP_HISTORY] Не удалось получить список файлов для tier {}: {}",
                        tier, e.getMessage(), e);
                continue;
            }

            for (String fileName : files) {
                BackupReport report = tryLoadReport(fileName, tier);
                if (report == null) {
                    continue;
                }

                if (statusFilter != null && report.getStatus() != statusFilter) {
                    continue;
                }

                result.add(toSummaryDto(report, fileName, tier));
            }
        }

        result.sort(Comparator.comparing(BackupReportSummaryResponse::startedAt).reversed());
        return result;
    }

    /**
     * Загружает полный отчёт по указанному файлу бэкапа.
     *
     * @param fileName имя файла бэкапа
     * @param tier     уровень хранения
     * @return десериализованный {@link BackupReport}
     * @throws IllegalStateException если отчёт отсутствует или не читается
     */
    public BackupReport loadReport(String fileName, BackupTier tier) {
        BackupReport report = tryLoadReport(fileName, tier);
        if (report == null) {
            throw new IllegalStateException(
                    "Не удалось прочитать report.json из файла " + fileName + " (tier=" + tier + ")"
            );
        }
        return report;
    }

    /**
     * Пытается загрузить {@link BackupReport} из ZIP‑файла бэкапа.
     * В случае ошибки или отсутствия report.json возвращает {@code null}.
     */
    private BackupReport tryLoadReport(String fileName, BackupTier tier) {
        try (InputStream in = backupFileService.load(fileName, tier)) {
            BackupReport report = archiveReader.readReport(in);
            if (report == null) {
                log.warn("[BACKUP_HISTORY] В архиве {} (tier={}) отсутствует report.json или он пустой",
                        fileName, tier);
            } else {
                log.debug("[BACKUP_HISTORY] Успешно прочитан report.json из {} (tier={}) со статусом {}",
                        fileName, tier, report.getStatus());
            }
            return report;
        } catch (IOException e) {
            log.warn("[BACKUP_HISTORY] Ошибка чтения отчёта из файла {} (tier={}): {}",
                    fileName, tier, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Преобразует полный отчёт {@link BackupReport} в краткую форму
     * {@link BackupReportSummaryResponse} для списка истории.
     */
    private BackupReportSummaryResponse toSummaryDto(BackupReport r, String fileName, BackupTier tier) {
        BackupSummary s = r.getSummary();
        return new BackupReportSummaryResponse(
                fileName,
                tier,
                r.getOperation(),
                r.getStatus(),
                r.getSchemaVersion(),
                r.getStartedAt(),
                r.getFinishedAt(),
                s != null ? s.getTotalEntities() : null,
                s != null ? s.getProcessed() : null,
                s != null ? s.getErrors() : null
        );
    }
}