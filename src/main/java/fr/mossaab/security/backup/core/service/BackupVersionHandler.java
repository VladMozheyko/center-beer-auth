package fr.mossaab.security.backup.core.service;


import com.fasterxml.jackson.core.JsonGenerator;
import fr.mossaab.security.backup.core.report.BackupReport;

import java.io.IOException;
import java.io.InputStream;

public interface BackupVersionHandler {

    int getSchemaVersion();

    /**
     * Экспортирует данные по всем сущностям
     * формируя и дополняя отчёт {@link BackupReport}.
     * <p>
     *
     * @param generator Jackson {@link JsonGenerator}, в который пишутся экспортируемые данные
     * @param report    объект отчёта, который будет дополнен сводной и детальной информацией
     *
     * @return тот же {@link BackupReport}, дополненный данными по экспорту
     *
     * @throws IOException если возникает ошибка при записи JSON
     */
    BackupReport exportData(JsonGenerator generator, BackupReport report) throws IOException;

    /**
     * Импортирует данные из JSON‑бэкапа и возвращает отчёт по операции восстановления.
     *
     * @param backupJsonInput входной поток с содержимым backup.json
     * @return отчёт о результате импорта
     * @throws IOException при ошибках чтения или парсинга JSON
     */
    BackupReport importData(InputStream backupJsonInput) throws IOException;
}
