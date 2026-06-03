package fr.mossaab.security.backup.core.service.impl;


import fr.mossaab.security.backup.core.config.BackupRetentionProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;

/**
 * Сервис применения политики хранения (retention) для резервных копий.
 * <p>
 * Для заданного уровня {@link BackupTier} удаляет бэкапы,
 * старше настроенного срока хранения из {@link BackupRetentionProperties}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupRetentionService {

    private final BackupFileService backupFileService;
    private final BackupRetentionProperties backupRetentionProperties;

    /**
     * Применяет политику хранения к указанному уровню:
     * удаляет все бэкапы, созданные ранее расчётной граничной даты.
     *
     * @param tier уровень хранения, к которому нужно применить retention
     */
    public void applyRetention(BackupTier tier) {
        try {
            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate cutoffDate = calculateCutoffDateForTier(tier, now);
            if (cutoffDate == null) {
                log.debug("[BACKUP_RETENTION] для уровня {} полика хранения не задана, удаление не выполняется", tier);
                return;
            }

            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

            log.debug("[BACKUP_RETENTION] уровень={} граничная дата удаления={}", tier, cutoffDate);

            for (String fileName : backupFileService.list(tier)) {
                try {
                    Instant created = backupFileService
                            .getCreationTime(fileName, tier)
                            .toInstant();

                    if (created.isBefore(cutoffInstant)) {
                        log.info("[BACKUP_RETENTION] удаляем старую резервную копию: уровень={} файл={}",
                                tier, fileName);
                        backupFileService.delete(fileName, tier);
                    }
                } catch (IOException e) {
                    log.warn("[BACKUP_RETENTION] не удалось обработать резервную копию: уровень={} файл={}",
                            tier, fileName, e);
                }
            }

        } catch (IOException e) {
            log.error("[BACKUP_RETENTION] ошибка при применении политики хранения для уровня={}", tier, e);
        }
    }

    /**
     * Рассчитывает граничную дату для удаления бэкапов для заданного уровня,
     * используя настройки из {@link BackupRetentionProperties}.
     *
     * @param tier уровень хранения
     * @param now  «текущая» дата (для удобства тестирования передаётся параметром)
     * @return дата, раньше которой бэкапы считаются устаревшими
     */
    private LocalDate calculateCutoffDateForTier(BackupTier tier, LocalDate now) {
        return switch (tier) {
            case DAILY      -> now.minusDays(backupRetentionProperties.getDailyDays());
            case WEEKLY     -> now.minusWeeks(backupRetentionProperties.getWeeklyWeeks());
            case MONTHLY    -> now.minusMonths(backupRetentionProperties.getMonthlyMonths());
            case SEMI_ANNUAL ->
                    now.minusMonths(backupRetentionProperties.getSemiAnnualYears() * 6L);
            case ANNUAL     -> now.minusYears(backupRetentionProperties.getAnnualYears());
        };
    }
}