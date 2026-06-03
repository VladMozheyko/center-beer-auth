package fr.mossaab.security.backup.core.service.impl;

import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.Period;
import java.util.Comparator;

/**
 * Сервис «продвижения» (promotion) бэкапа между уровнями хранения.
 * <p>
 * Из заданного {@code fromTier} выбирает самый новый успешный бэкап,
 * созданный за указанный {@link Period}, и копирует его в {@code toTier}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupPromotionService {

    private final BackupFileService backupFileService;
    private final BackupArchiveReader backupArchiveReader;

    /**
     * Находит в {@code fromTier} самый новый успешный бэкап за указанный период и копирует его в {@code toTier}.
     * Если подходящих бэкапов нет, просто пишет предупреждение в лог.
     *
     * @param fromTier исходный уровень хранения (откуда брать бэкап)
     * @param toTier   целевой уровень хранения (куда копировать)
     * @param period   максимальная «давность» бэкапа (например, {@code Period.ofDays(1)})
     */
    public void promote(BackupTier fromTier, BackupTier toTier, Period period) {
        try {
            Instant now = Instant.now();
            Instant fromInstant = now.minus(period);

            log.info("[BACKUP_PROMOTION] Запуск promotion: from={} to={} period={}", fromTier, toTier, period);

            var files = backupFileService.list(fromTier);

            String candidate = files.stream()
                    // 1. только успешные бэкапы
                    .filter(fileName -> isSuccessful(fileName, fromTier))
                    // 2. только в окне [fromInstant, now]
                    .filter(fileName -> {
                        try {
                            Instant created = backupFileService
                                    .getCreationTime(fileName, fromTier)
                                    .toInstant();
                            return !created.isBefore(fromInstant) && !created.isAfter(now);
                        } catch (IOException e) {
                            log.warn("[BACKUP_PROMOTION] Не удалось прочитать атрибуты файла {} (tier={}): {}",
                                    fileName, fromTier, e.getMessage(), e);
                            return false;
                        }
                    })
                    // 3. самый новый по дате создания
                    .max(Comparator.comparing(fileName -> {
                        try {
                            return backupFileService
                                    .getCreationTime(fileName, fromTier)
                                    .toInstant();
                        } catch (IOException e) {
                            log.warn("[BACKUP_PROMOTION] Ошибка при повторном чтении атрибутов файла {} (tier={}): {}",
                                    fileName, fromTier, e.getMessage(), e);
                            return Instant.EPOCH;
                        }
                    }))
                    .orElse(null);

            if (candidate == null) {
                log.warn("[BACKUP_PROMOTION] Нет подходящих SUCCESSFUL бэкапов для promotion из {} в {} за период {}",
                        fromTier, toTier, period);
                return;
            }

            backupFileService.copy(candidate, fromTier, toTier);
            log.info("[BACKUP_PROMOTION] Успешный promotion бэкапа {} из {} в {}", candidate, fromTier, toTier);

        } catch (Exception e) {
            log.error("[BACKUP_PROMOTION] Необработанная ошибка promotion из {} в {}", fromTier, toTier, e);
        }
    }

    /**
     * Проверяет по report.json, является ли бэкап успешным.
     *
     * @param fileName имя файла бэкапа
     * @param tier     уровень хранения
     * @return {@code true}, если бэкап помечен как успешный; иначе {@code false}
     */
    protected boolean isSuccessful(String fileName, BackupTier tier) {
        try (InputStream in = backupFileService.load(fileName, tier)) {
            boolean success = backupArchiveReader.isBackupSuccessful(in);
            if (!success) {
                log.debug("[BACKUP_PROMOTION] Бэкап {} на уровне {} не отмечен как SUCCESS", fileName, tier);
            }
            return success;
        } catch (IOException e) {
            log.warn("[BACKUP_PROMOTION] Не удалось прочитать отчёт для бэкапа {} на уровне {}: {}",
                    fileName, tier, e.getMessage(), e);
            return false;
        }
    }
}