package fr.mossaab.security.backup.core.service;

import fr.mossaab.security.backup.core.enums.BackupTier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Сервис низкоуровневой работы с файлами резервных копий.
 * <p>
 * Цель интерфейса — инкапсулировать детали файловой системы (пути, атрибуты и т.п.)
 * и предоставить единое API для:
 * <ul>
 *     <li>получения списка файлов в заданном уровне хранения ({@link BackupTier});</li>
 *     <li>загрузки содержимого файла в виде {@link InputStream};</li>
 *     <li>удаления файла;</li>
 *     <li>получения метаданных (в частности — времени создания);</li>
 *     <li>копирования файла между уровнями хранения.</li>
 * </ul>
 * Все операции работают с логическими уровнями (tier), а не с абсолютными путями.
 */
public interface BackupFileService {

    /**
     * Возвращает список имён файлов резервных копий в указанном уровне хранения.
     *
     * @param tier уровень хранения резервных копий (например, HOURLY, DAILY, MONTHLY)
     * @return отсортированный список имён файлов (без путей); пустой список, если каталог отсутствует или пуст
     * @throws IOException при ошибке доступа к хранилищу (файловая система, сеть и т.п.)
     */
    List<String> list(BackupTier tier) throws IOException;

    /**
     * Открывает поток для чтения содержимого файла резервной копии.
     * <p>
     * ВНИМАНИЕ: вызывающий код обязан закрыть возвращаемый {@link InputStream}.
     *
     * @param fileName логическое имя файла (как возвращает {@link #list(BackupTier)})
     * @param tier     уровень хранения, в котором находится файл
     * @return входной поток для чтения содержимого файла
     * @throws IOException если файл не существует или произошла ошибка ввода-вывода
     */
    InputStream load(String fileName, BackupTier tier) throws IOException;

    /**
     * Удаляет файл резервной копии в указанном уровне хранения.
     *
     * @param fileName имя файла для удаления
     * @param tier     уровень хранения, в котором находится файл
     * @throws IOException при ошибке удаления (недостаточно прав, проблемы с ФС и т.п.)
     */
    void delete(String fileName, BackupTier tier) throws IOException;

    /**
     * Возвращает время создания файла резервной копии в указанном уровне.
     *
     * @param fileName имя файла
     * @param tier     уровень хранения, в котором находится файл
     * @return время создания файла в виде {@link FileTime}
     * @throws IOException при ошибке чтения атрибутов файла
     */
    FileTime getCreationTime(String fileName, BackupTier tier) throws IOException;

    /**
     * Копирует файл резервной копии из одного уровня хранения в другой.
     * <p>
     * Если файл с таким именем в целевом уровне уже существует, он будет перезаписан.
     * Реализация обязана создать каталог целевого уровня, если он отсутствует.
     *
     * @param fileName имя файла, который нужно скопировать
     * @param fromTier исходный уровень хранения
     * @param toTier   целевой уровень хранения
     * @throws IOException при ошибке чтения/записи файлов или создания каталогов
     */
    void copy(String fileName, BackupTier fromTier, BackupTier toTier) throws IOException;
}