package fr.mossaab.security.backup.core.service;


import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.enums.BackupTier;

public interface BackupService {

    /**
     * Создаёт полный бекап системы и сохраняет его через BackupStorage.
     * Возвращает JSON-отчёт о ходе операции.
     */
    BackupReport exportSystemBackup();

    /**
     * Восстанавливает систему из ранее сохранённого бекапа.
     * Возвращает JSON-отчёт.
     */
    BackupReport restoreSystemBackup(String fileName, BackupTier backupTier);
}
