package fr.mossaab.security.backup.core.scheduler;

import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.impl.BackupPromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Period;

/**
 * Планировщик задач продвижения бэкапов между уровнями хранения.
 * <p>
 * Отвечает за регулярное копирование (promotion) наиболее удачных бекапов
 * в более старшие уровни:
 * <ul>
 *     <li>{@link BackupTier#DAILY} → {@link BackupTier#WEEKLY}</li>
 *     <li>{@link BackupTier#WEEKLY} → {@link BackupTier#MONTHLY}</li>
 *     <li>{@link BackupTier#MONTHLY} → {@link BackupTier#SEMI_ANNUAL}</li>
 *     <li>{@link BackupTier#SEMI_ANNUAL} → {@link BackupTier#ANNUAL}</li>
 * </ul>
 * CRON-расписания и часовой пояс берутся из {@code BackupScheduleProperties}
 * через SpEL-выражения в аннотациях {@link Scheduled}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupPromotionScheduler {

    private final BackupPromotionService backupPromotionService;

    /**
     * Раз в день пробуем промотировать DAILY → WEEKLY за последние 7 дней.
     * Метод идемпотентный, так что частый запуск безопасен
     * (если нечего промотировать — просто ничего не сделает).
     */
    @Scheduled(cron = "#{@backupScheduleProperties.promoteDailyToWeeklyCron}",
            zone = "#{@backupScheduleProperties.zoneId}") //  UTC
    public void promoteDailyToWeekly() {
        log.info("[BACKUP_PROMOTION_SCHEDULER] запуск promotion DAILY → WEEKLY");
        backupPromotionService.promote(
                BackupTier.DAILY,
                BackupTier.WEEKLY,
                Period.ofDays(7)
        );
    }

    /**
     * Раз в день пробуем промотировать WEEKLY → MONTHLY за последние 31 день.
     * Можно ограничить запуск первыми числами месяца, но часто проще запускать
     * ежедневно — сервис сам выберет 'самый новый успешный' в рамках периода.
     */
    @Scheduled(cron = "#{@backupScheduleProperties.promoteWeeklyToMonthlyCron}",
            zone = "#{@backupScheduleProperties.zoneId}") // ежедневно в 02:05 UTC
    public void promoteWeeklyToMonthly() {
        log.info("[BACKUP_PROMOTION_SCHEDULER] запуск promotion WEEKLY → MONTHLY");
        backupPromotionService.promote(
                BackupTier.WEEKLY,
                BackupTier.MONTHLY,
                Period.ofDays(31)
        );
    }

    /**
     * Раз в день пробуем промотировать MONTHLY → SEMI_ANNUAL за последние 6 месяцев.
     */
    @Scheduled(cron = "#{@backupScheduleProperties.promoteMonthlyToSemiAnnualCron}",
            zone = "#{@backupScheduleProperties.zoneId}")
    public void promoteMonthlyToSemiAnnual() {
        log.info("[BACKUP_PROMOTION_SCHEDULER] запуск promotion MONTHLY → SEMI_ANNUAL");
        backupPromotionService.promote(
                BackupTier.MONTHLY,
                BackupTier.SEMI_ANNUAL,
                Period.ofMonths(6)
        );
    }

    /**
     * Раз в день пробуем промотировать SEMI_ANNUAL → ANNUAL за последние 12 месяцев.
     */
    @Scheduled(cron = "#{@backupScheduleProperties.promoteSemiAnnualToAnnualCron}",
            zone = "#{@backupScheduleProperties.zoneId}")
    public void promoteSemiAnnualToAnnual() {
        log.info("[BACKUP_PROMOTION_SCHEDULER] запуск promotion SEMI_ANNUAL → ANNUAL");
        backupPromotionService.promote(
                BackupTier.SEMI_ANNUAL,
                BackupTier.ANNUAL,
                Period.ofYears(1)
        );
    }
}
