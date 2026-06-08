package fr.mossaab.security.backup.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация расписаний для задач бэкапа.
 * <p>
 * Связывается с настройками с префиксом {@code backup.schedule} в конфигурации
 * приложения (application.yml / application.properties).
 */
@Data
@Component
@ConfigurationProperties(prefix = "backup.schedule")
public class BackupScheduleProperties {

    /**
     * CRON‑выражение для запуска ежедневного бэкапа.
     */
    private String dailyBackupCron;

    /**
     * CRON‑выражение для задачи, повышающей ежедневные бэкапы до недельных.
     */
    private String promoteDailyToWeeklyCron;

    /**
     * CRON‑выражение для задачи, повышающей недельные бэкапы до месячных.
     */
    private String promoteWeeklyToMonthlyCron;

    /**
     * CRON‑выражение для задачи, повышающей месячные бэкапы до полугодовых.
     */
    private String promoteMonthlyToSemiAnnualCron;

    /**
     * CRON‑выражение для задачи, повышающей полугодовые бэкапы до годовых.
     */
    private String promoteSemiAnnualToAnnualCron;

    /**
     * CRON‑выражение для задачи очистки старых бэкапов
     * согласно политике хранения (retention).
     */
    private String retentionCron;

    /**
     * Идентификатор часового пояса, в котором интерпретируются
     * все CRON‑выражения.
     * Значение по умолчанию: {@code Europe/Moscow}.
     */
    private String zoneId = "Europe/Moscow";
}
