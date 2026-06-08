package fr.mossaab.security.backup.core.storage;


import fr.mossaab.security.backup.core.enums.BackupTier;

import java.io.InputStream;
import java.io.IOException;

/**
 * Абстракция над хранилищем резервных копий (файловый диск, S3, FTP и т.п.).
 * Позволяет сохранять, загружать и удалять файлы бэкапов по уровню хранения.
 */
public interface BackupStorage {

    /**
     * Сохранить бэкап.
     *
     * @param tier уровень хранения (LOCAL, REMOTE и т.п.)
     * @param backupName имя файла (например, my-backup-2024-05-20.zip)
     * @param content поток данных; реализатор обязан прочитать его полностью и закрыть
     */
    void save(BackupTier tier, String backupName, InputStream content) throws IOException;

    /**
     * Загрузить бэкап.
     *
     * @param backupName имя файла
     * @param tier уровень хранения
     * @return InputStream, который вызывающий обязан закрыть
     */
    InputStream load(String backupName, BackupTier tier) throws IOException;

    /**
     * Удалить файл бэкапа из хранилища.
     *
     * @param tier       уровень хранения
     * @param backupName имя файла
     * @throws IOException при ошибках удаления или доступа к хранилищу
     */
    void delete(BackupTier tier, String backupName) throws IOException;
}
