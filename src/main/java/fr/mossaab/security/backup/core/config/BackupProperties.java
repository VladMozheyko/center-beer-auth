package fr.mossaab.security.backup.core.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Общие настройки механизма бэкапов.
 *<p>Читает значения из конфигурации по префиксу "backup".</p>
 */
@ConfigurationProperties(prefix = "backup")
@Configuration
@Data
public class BackupProperties {

    /** Текущая версия схемы формата бэкапа (для совместимости при изменениях структуры) */
    private int currentSchemaVersion = 1;

    /** Версия приложения */
    private String appVersion = "0.0.0";

    /** Тип хранилища бэкапов (локальная ФС, S3 и т.п.) */
    private StorageType storageType = StorageType.FILE_SYSTEM;

    private boolean formattedJson = true;

    /** Настройки для хранения бэкапов в файловой системе. */
    private FileSystemStorageProperties fileSystem = new FileSystemStorageProperties();

    public enum StorageType {
        FILE_SYSTEM,
        S3 //на будущее расширение
    }

    @Data
    public static class FileSystemStorageProperties {
        private String basePath;
    }
}
