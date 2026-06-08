package fr.mossaab.security.backup.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Настройки политики хранения (retention) для разных уровней бэкапов.
 *
 * <p>Читает значения из конфигурации по префиксу "backup.retention".
 * Используется сервисом retention для решения, какие бэкапы считать
 * «просроченными» и удалять.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "backup.retention")
public class BackupRetentionProperties {

    /** Сколько дней хранить ежедневные (DAILY) бэкапы. */
    private int dailyDays = 14;

    /** Сколько недель хранить еженедельные (WEEKLY) бэкапы. */
    private int weeklyWeeks = 12;

    /** Сколько месяцев хранить ежемесячные (MONTHLY) бэкапы. */
    private int monthlyMonths = 24;

    /** Сколько лет хранить полугодовые (SEMI_ANNUAL) бэкапы. */
    private int semiAnnualYears = 5;

    /** Сколько лет хранить годовые (ANNUAL) бэкапы. */
    private int annualYears = 10;
}