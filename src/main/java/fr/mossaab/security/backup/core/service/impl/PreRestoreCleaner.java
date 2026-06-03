package fr.mossaab.security.backup.core.service.impl;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

/**
 * Сервис для очистки всех таблиц, соответствующих JPA‑сущностям, перед восстановлением данных из бэкапа.
 *
 * <p>Особенности работы:</p>
 * <ul>
 *   <li>выполняет очистку в отдельной транзакции ({@link Propagation#REQUIRES_NEW});</li>
 *   <li>временно отключает проверки внешних ключей (FK) в MySQL на время очистки;</li>
 *   <li>сбрасывает последовательности (sequences) для корректной генерации ID после импорта;</li>
 *   <li>при любой ошибке во время очистки или сброса последовательностей транзакция откатывается,
 *       исключение пробрасывается наверх для обработки на более высоком уровне.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreRestoreCleaner {

    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Основной метод очистки всех entity‑таблиц перед восстановлением из бэкапа.
     *
     * <p>Выполняемые действия:</p>
     * <ol>
     *   <li>получение списка всех JPA‑сущностей через {@link EntityManager#getMetamodel()};</li>
     *   <li>отключение проверок внешних ключей ({@code FOREIGN_KEY_CHECKS = 0});</li>
     *   <li>поочерёдная очистка каждой таблицы через {@code DELETE FROM};</li>
     *   <li>сброс всех последовательностей ({@link #resetAllSequences()});</li>
     *   <li>включение проверок внешних ключей обратно ({@code FOREIGN_KEY_CHECKS = 1});</li>
     *   <li>очистка кэша {@link EntityManager}.</li>
     * </ol>
     *
     * <p><b>Важно:</b> при любой ошибке (очистка таблицы, сброс последовательности и т. д.)
     * транзакция будет откачена, исключение проброшено наверх для обработки вызывающим кодом.</p>
     *
     * @throws RuntimeException если произошла ошибка при очистке таблиц или сбросе последовательностей
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanAllEntities() {
        Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
        log.info("[GENERIC_TABLES_CLEANER] Найдено {} сущностей", entities.size());

        // Отключаем FK (MySQL)
        disableForeignKeyChecks();

        try {
            for (EntityType<?> entityType : entities) {
                Class<?> javaType = entityType.getJavaType();
                String tableName = resolveTableName(javaType);
                if (tableName == null) {
                    log.warn("[GENERIC_TABLES_CLEANER] Не удалось определить имя таблицы для {} – пропускаем",
                            javaType.getName());
                    continue;
                }

                if (!tableExists(tableName)) {
                    log.warn("[GENERIC_TABLES_CLEANER] Таблица '{}' не существует – пропускаем", tableName);
                    continue;
                }

                log.debug("[GENERIC_TABLES_CLEANER] Очистка таблицы '{}', сущность={}",
                        tableName, javaType.getSimpleName());

                jdbcTemplate.update("DELETE FROM " + tableName);
            }

//            resetAllSequences();
            log.info("[GENERIC_TABLES_CLEANER] Очистка всех entity-таблиц завершена");
        } finally {
            // Всегда включаем FK обратно, даже если была ошибка
            enableForeignKeyChecks();
            entityManager.flush();
            entityManager.clear();
        }
    }

    /**
     * Определяет имя таблицы БД для заданной JPA‑сущности.
     *
     * <p>Порядок определения:</p>
     * <ol>
     *   <li>значение {@link Table#name}, если задано;</li>
     *   <li>значение {@link Entity#name}, если задано;</li>
     *   <li>автоматически сгенерированное имя в формате {@code snake_case} из имени класса
     *       (например, {@code MyUserProfile} → {@code my_user_profile}).</li>
     * </ol>
     *
     * @param entityClass класс JPA‑сущности
     * @return имя таблицы или {@code null}, если определить не удалось
     */
    private String resolveTableName(Class<?> entityClass) {
        // 1. @Table(name)
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && table.name() != null && !table.name().isBlank()) {
            return table.name();
        }

        // 2. @Entity(name)
        Entity entity = entityClass.getAnnotation(Entity.class);
        if (entity != null && entity.name() != null && !entity.name().isBlank()) {
            return entity.name();
        }

        // 3. Генерация из имени класса: MyUserProfile -> my_user_profile
        String generated = toSnakeCase(entityClass.getSimpleName());
        log.debug("[GENERIC_TABLES_CLEANER] Для сущности {} сгенерировано имя таблицы '{}'",
                entityClass.getName(), generated);
        return generated;
    }

    /**
     * Преобразует имя класса из формата camelCase в snake_case.
     *
     * <p>Логика преобразования:</p>
     * <ul>
     *   <li>добавляет символ {@code _} перед заглавной буквой, если перед ней строчная
     *       или после неё строчная;</li>
     *   <li>переводит все символы в нижний регистр;</li>
     *   <li>обрабатывает крайние случаи (начало строки, последовательности заглавных букв).</li>
     * </ul>
     *
     * @param className исходное имя класса в camelCase
     * @return преобразованное имя в формате snake_case (например,
     *         {@code MyUserProfile} → {@code my_user_profile})
     */
    private String toSnakeCase(String className) {
        StringBuilder result = new StringBuilder();
        char[] chars = className.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (Character.isUpperCase(c)) {
                // не добавляем '_' в самом начале
                if (i > 0 && (Character.isLowerCase(chars[i - 1]) ||
                        (i + 1 < chars.length && Character.isLowerCase(chars[i + 1])))){
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * Проверяет существование таблицы в текущей базе данных.
     *
     * <p>Выполняет SQL‑запрос к {@code INFORMATION_SCHEMA.TABLES} для поиска таблицы
     * по имени.</p>
     *
     * @param tableName имя таблицы для проверки
     * @return {@code true}, если таблица существует, иначе {@code false}
     * @throws RuntimeException при ошибке выполнения SQL‑запроса
     */
    @Transactional(readOnly = true)
    public boolean tableExists(String tableName) {
        try {
            String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
            """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("[GENERIC_TABLES_CLEANER] Ошибка при проверке существования таблицы '{}'", tableName, e);
            throw new RuntimeException("Ошибка при проверке существования таблицы '" + tableName + "'", e);
        }
    }

    /**
     * Отключает проверку внешних ключей в MySQL ({@code FOREIGN_KEY_CHECKS = 0}).
     *
     * <p>Используется перед началом очистки таблиц, чтобы избежать ошибок из‑за
     * ограничений FK.</p>
     *
     * @throws RuntimeException если не удалось отключить проверки внешних ключей
     */
    private void disableForeignKeyChecks() {
        try {
            log.debug("[GENERIC_TABLES_CLEANER] Отключаем FOREIGN_KEY_CHECKS");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        } catch (Exception e) {
            log.error("[GENERIC_TABLES_CLEANER] Критическая ошибка: не удалось отключить FOREIGN_KEY_CHECKS", e);
            throw new RuntimeException("Не удалось отключить проверки внешних ключей", e);
        }
    }

    /**
     * Сбрасывает все последовательности (sequence tables) в БД.
     *
     * <p>Выполняемые действия:</p>
     * <ol>
     *   <li>поиск таблиц с именами, заканчивающимися на {@code _seq}
     *       (через {@code INFORMATION_SCHEMA.TABLES});</li>
     *   <li>удаление всех записей из каждой найденной таблицы последовательности;</li>
     *   <li>вставка новой записи со значением {@code next_val = 1}.</li>
     * </ol>
     *
     * <p>Цель: обеспечить генерацию ID с начала после восстановления данных.</p>
     *
     * @throws RuntimeException если произошла ошибка при сбросе последовательностей
     */
    private void resetAllSequences() {
        String sql = """
        SELECT TABLE_NAME
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME LIKE '%\\\\_seq' ESCAPE '\\\\';
    """;

        try {
            var seqTableNames = jdbcTemplate.queryForList(sql, String.class);

            for (String seqTable : seqTableNames) {
                log.debug("[GENERIC_TABLES_CLEANER] Сброс последовательности в таблице '{}'", seqTable);

                jdbcTemplate.update("DELETE FROM " + seqTable);
                jdbcTemplate.update("INSERT INTO " + seqTable + " (next_val) VALUES (1)");
            }
            log.debug("[GENERIC_TABLES_CLEANER] Сброс последовательностей завершён для {} таблиц", seqTableNames.size());
        } catch (Exception e) {
            log.error("[GENERIC_TABLES_CLEANER] Ошибка при сбросе последовательностей", e);
            throw new RuntimeException("Ошибка при сбросе последовательностей в БД", e);
        }
    }

    /**
     * Включает проверку внешних ключей в MySQL обратно ({@code FOREIGN_KEY_CHECKS = 1}).
     *
     * <p>Вызывается в блоке {@code finally} метода {@link #cleanAllEntities()},
     * чтобы гарантировать включение FK даже при ошибках.</p>
     *
     * <p><b>Важно:</b> если не удаётся включить проверки внешних ключей, это логируется,
     * но исключение не пробрасывается, чтобы не маскировать основную ошибку.</p>
     */
    private void enableForeignKeyChecks() {
        try {
            log.debug("[GENERIC_TABLES_CLEANER] Включаем FOREIGN_KEY_CHECKS");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        } catch (Exception e) {
            log.warn("[GENERIC_TABLES_CLEANER] Предупреждение: не удалось включить FOREIGN_KEY_CHECKS. "
                    + "Это может привести к проблемам с целостностью данных.", e);
        }
    }
}