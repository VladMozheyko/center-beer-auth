package fr.mossaab.security.backup.core.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import fr.mossaab.security.backup.core.enums.BackupEntityStatus;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupOperationType;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.report.BackupReportEntry;
import fr.mossaab.security.backup.core.report.BackupSummary;
import fr.mossaab.security.backup.core.report.BackupReportCounter;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBackupVersionHandler implements BackupVersionHandler {

    protected final ObjectMapper objectMapper;

    /**
     * Наследник должен вернуть список «описаний сущностей для экспорта».
     */
    protected abstract List<ExportTask<?, ?>> getExportTasks();

    /**
     * Экспортирует данные по всем сущностям, описанным в {@link #getExportTasks()},
     * формируя и дополняя отчёт {@link BackupReport}.
     * <p>
     * Алгоритм по умолчанию:
     * <ol>
     *     <li>Создаёт {@link ObjectWriter} из {@link #objectMapper} для сериализации DTO.</li>
     *     <li>Для каждой задачи из {@link #getExportTasks()} вызывает {@link #exportOneTask(JsonGenerator, ObjectWriter, ExportTask)}</li>
     *     <li>Собирает список счётчиков {@link BackupReportCounter} по всем задачам.</li>
     *     <li>Агрегирует сводную статистику через {@link #fillSummary(List, BackupSummary)} и записывает её в {@link BackupSummary} внутри отчёта.</li>
     *     <li>Добавляет детализированные отчёты (counters) в {@link BackupReport#getDetails()}.</li>
     * </ol>
     * Если конкретной версии бэкапа нужен другой алгоритм экспорта — метод можно переопределить.
     *
     * @param generator Jackson {@link JsonGenerator}, в который пишутся экспортируемые данные
     * @param report    объект отчёта, который будет дополнен сводной и детальной информацией
     *
     * @return тот же {@link BackupReport}, дополненный данными по экспорту
     *
     * @throws IOException если возникает ошибка при записи JSON
     */
    @Override
    public BackupReport exportData(JsonGenerator generator, BackupReport report) throws IOException {
        ObjectWriter writer = objectMapper.writer();

        List<BackupReportCounter> counters = new ArrayList<>();

        for (ExportTask<?, ?> rawTask : getExportTasks()) {
            counters.add(exportOneTask(generator, writer, rawTask));
        }

        BackupSummary summary = report.getSummary();
        if (summary == null) {
            summary = new BackupSummary();
            report.setSummary(summary);
        }
        fillSummary(counters, summary);

        // добавляем детализацию
        if (report.getDetails() == null) {
            report.setDetails(new ArrayList<>());
        }
        report.getDetails().addAll(counters);

        return report;
    }

    /**
     * Заполняет сводный отчёт по результатам экспорта.
     * <p>
     * Агрегирует статистику по всем сущностям из списка {@link BackupReportCounter}
     * и записывает её в {@link BackupSummary}.
     * </p><p>
     * Считаются:
     * <ul>
     *     <li>общее количество обработанных сущностей (total);</li>
     *     <li>количество успешно экспортированных (processed/exported);</li>
     *     <li>количество пропущенных (skipped);</li>
     *     <li>количество ошибок (errors) по записям со статусом {@link BackupEntityStatus#ERROR}.</li>
     * </ul>
     *
     * @param reports список отчётов по отдельным задачам экспорта
     * @param summary сводный объект, в который записывается агрегированная статистика
     */
    protected void fillSummary(List<BackupReportCounter> reports, BackupSummary summary) {
        long total = 0;
        long processed = 0;
        long skipped = 0;
        long errors = 0;

        for (BackupReportCounter r : reports) {
            total += r.getTotal();
            processed += r.getExported();
            skipped += r.getSkipped();
            if (r.getDetails() != null) {
                errors += r.getDetails().stream()
                        .filter(e -> e.getStatus() == BackupEntityStatus.ERROR)
                        .count();
            }
        }

        summary.setTotalEntities(total);
        summary.setProcessed(processed);
        summary.setSkipped(skipped);
        summary.setErrors((int) errors);
    }

    /**
     * Выполняет экспорт одной задачи {@link ExportTask}.
     * <p>
     * Приводит сырой {@code ExportTask<?, ?>} к параметризованному виду и
     * делегирует работу методу {@link #exportEntitiesInBatches(JsonGenerator, ObjectWriter, String, JpaRepository, Function, Function)}.
     *
     * @param generator Jackson {@link JsonGenerator}, в который пишется JSON текущей сущности
     * @param writer    Jackson {@link ObjectWriter}, используемый для сериализации DTO
     * @param rawTask   задача экспорта без конкретных дженериков (как хранятся в списке задач)
     * @param <T>       тип экспортируемой сущности
     * @param <D>       тип DTO, в который маппится сущность
     *
     * @return отчёт по экспорту одной сущности ({@link BackupReportCounter})
     *
     * @throws IOException если возникает ошибка при записи JSON
     */
    @SuppressWarnings("unchecked")
    private <T, D> BackupReportCounter exportOneTask(
            JsonGenerator generator,
            ObjectWriter writer,
            ExportTask<?, ?> rawTask
    ) throws IOException {
        ExportTask<T, D> task = (ExportTask<T, D>) rawTask;
        return exportEntitiesInBatches(
                generator,
                writer,
                task.entityName(),
                task.repository(),
                task.mapper(),
                task.idExtractor()
        );
    }

    /**
     * Экспортирует сущности заданного типа батчами в JSON‑массив, формируя отчёт по результатам.
     * <p>
     * Читает данные постранично из репозитория, маппит сущности в DTO, пишет их в JSON
     * и накапливает статистику (total/exported/skipped) и детали по пропущенным сущностям.
     *
     * @param generator   Jackson {@link JsonGenerator}, в который пишется JSON‑массив сущностей
     * @param writer      Jackson {@link ObjectWriter}, используемый для сериализации DTO в JSON
     * @param entityName  имя поля/массива в JSON (и имя сущности в логах/отчёте)
     * @param repository  репозиторий, из которого постранично читаются сущности
     * @param mapper      функция преобразования сущности {@code T} в DTO {@code D} для экспорта
     * @param idExtractor функция, безопасно извлекающая строковый идентификатор сущности {@code T}
     *                    (используется в логах и отчёте; обёрнута через {@link #safeGetId})
     * @param <T>         тип экспортируемой сущности (Entity)
     * @param <D>         тип DTO, который уходит в бэкап
     *
     * @return заполненный {@link BackupReportCounter} с числом всего записей, успешно экспортированных
     *         и пропущенных, а также списком деталей по пропущенным сущностям
     *
     * @throws IOException если возникает ошибка при записи JSON через {@code generator} / {@code writer}
     */
    protected <T, D> BackupReportCounter exportEntitiesInBatches(
            JsonGenerator generator,
            ObjectWriter writer,
            String entityName,
            JpaRepository<T, ?> repository,
            Function<T, D> mapper,
            Function<T, String> idExtractor
    ) throws IOException {

        int pageSize = 1000;
        Pageable pageable = PageRequest.of(0, pageSize);

        long total = 0;
        long exported = 0;
        long skipped = 0;

        BackupReportCounter report = new BackupReportCounter();
        report.setEntityName(entityName);

        log.info("[EXPORT] Start export of entities for field '{}' with pageSize={}", entityName, pageSize);

        generator.writeArrayFieldStart(entityName);

        try {
            while (true) {
                Page<T> page;
                try {
                    page = repository.findAll(pageable);
                } catch (Exception e) {
                    log.error("[EXPORT] Failed to fetch page {} for field '{}'", pageable.getPageNumber(), entityName, e);
                    throw e;
                }

                if (!page.hasContent()) {
                    break;
                }


                for (T entity : page.getContent()) {
                    total++;
                    String originalId = null;
                    try {
                        originalId = safeGetId(entity, idExtractor);
                        D dto = mapper.apply(entity);
                        writer.writeValue(generator, dto);
                        exported++;
                    } catch (Exception e) {
                        skipped++;

                        String reasonMessage = e.getMessage() != null
                                ? e.getMessage()
                                : e.getClass().getSimpleName();

                        BackupReportEntry reportEntry = BackupReportEntry.builder()
                                .originalId(originalId)
                                .status(BackupEntityStatus.SKIPPED)
                                .reasonCode(null)
                                .reasonMessage(reasonMessage)
                                .build();
                        report.getDetails().add(reportEntry);

                        log.warn(
                                "[EXPORT_exportEntitiesInBatches] Entity with id='{}' skipped while exporting field '{}': {}",
                                originalId,
                                entityName,
                                reasonMessage,
                                e
                        );
                    }
                }

                if (!page.hasNext()) {
                    break;
                }
                pageable = page.nextPageable();
            }
        } finally {
            generator.writeEndArray();

            log.info(
                    "[EXPORT] Finished export for field '{}': total={}, exported={}, skipped={}",
                    entityName, total, exported, skipped
            );
        }

        report.setExported(exported);
        report.setSkipped(skipped);
        report.setTotal(total);

        return report;
    }

    /**
     * Безопасно извлекает строковый идентификатор сущности.
     * <p>
     * Если при извлечении id происходит ошибка или вернулся null,
     * логирует предупреждение и возвращает "unknown".
     *
     * @param entity       сущность, для которой нужно получить id
     * @param idExtractor  функция, извлекающая строковый идентификатор из сущности
     * @param <T>          тип сущности
     * @return строковый id сущности или "unknown" при ошибке/отсутствии id
     */
    protected <T> String safeGetId(T entity, Function<T, String> idExtractor) {
        try {
            String id = idExtractor.apply(entity);
            return id != null ? id : "unknown";
        } catch (Exception e) {
            log.warn("[EXPORT] Failed to extract id for entity of type {}: {}",
                    entity != null ? entity.getClass().getName() : "null", e.getMessage(), e);
            return "unknown";
        }
    }

    /**
     * Маленькая record, описывает одну сущность для экспорта.
     * Задаёт: откуда читать данные, как маппить их в DTO и как получать строковый id.
     *
     * @param entityName  человекочитаемое имя сущности/раздела бэкапа (например, "users")
     * @param repository  репозиторий, из которого читаются все сущности T для экспорта
     * @param mapper      функция маппинга сущности T в DTO D (для записи в бэкап)
     * @param idExtractor функция, извлекающая строковый идентификатор сущности T
     *                    (используется, например, для логирования или генерации ключей)
     * @param <T>         тип экспортируемой сущности (Entity)
     * @param <D>         тип DTO для бэкапа
     */
    public record ExportTask<T, D>(
            String entityName,
            JpaRepository<T, ?> repository,
            ExportMapper<T, D> mapper,
            ExportIdExtractor<T> idExtractor
    ) {}


    // ---------- IMPORT: общая часть ----------

    /**
     * Тип "конверта" с данными бэкапа (BackupDataV1 / BackupDataV2 / ...).
     */
    protected abstract Class<?> getEnvelopeClass();

    /**
     * Из envelope достать объект-описатель: списки DTO и пр.
     * Чтобы Abstract-класс ничего не знал про конкретные DTO.
     */
    protected abstract ImportEnvelopeDescriptor buildImportDescriptor(Object envelope);

    /**
     * Импортирует данные из JSON‑бэкапа и формирует отчёт о выполненной операции.
     * <p>
     * Общий алгоритм:
     * <ol>
     *   <li>Создаёт {@link BackupReport} со статусом операции {@link BackupOperationType#RESTORE}.</li>
     *   <li>Читает JSON из {@code backupJsonInput} как "обёртку" (envelope) типа {@link #getEnvelopeClass()}.</li>
     *   <li>Если обёртка пуста — помечает операцию как {@link BackupOperationStatus#FAILED} и возвращает отчёт.</li>
     *   <li>Строит {@link ImportEnvelopeDescriptor} через {@link #buildImportDescriptor(Object)}, в котором описаны:
     *       <ul>
     *         <li>списки задач простого импорта (без связей) — {@code simpleImportTasks};</li>
     *         <li>задачи восстановления связей — {@code relationTasks}.</li>
     *       </ul>
     *   </li>
     *   <li>Выполняет по очереди все {@link SimpleImportTask} через {@link #executeSimpleImport(SimpleImportTask)}:
     *       накапливает количество обработанных, ошибочных и всего записей.</li>
     *   <li>Затем выполняет все {@link RelationRestoreTask}, восстанавливая связи между сущностями;
     *       при ошибке восстановления связи увеличивает счётчик ошибок и пишет в лог.</li>
     *   <li>Заполняет {@link BackupSummary} (total, processed, skipped, renamed, errors)
     *       и выставляет статус {@link BackupOperationStatus#SUCCESS} или
     *       {@link BackupOperationStatus#COMPLETED_WITH_WARNINGS} в зависимости от наличия ошибок.</li>
     *   <li>В случае ошибки чтения JSON (IOException) выставляет статус {@link BackupOperationStatus#FAILED} и заполняет summary нулями.</li>
     * </ol>
     *
     * Метод помечен {@link Transactional}, поэтому вся операция импорта выполняется в одной транзакции
     * (в зависимости от настроек transaction manager).
     *
     * @param backupJsonInput входной поток с содержимым backup.json
     * @return заполненный {@link BackupReport} с итогами импорта
     */
    @Override
    @Transactional
    public BackupReport importData(InputStream backupJsonInput) {
        BackupReport report = new BackupReport();
        report.setOperation(BackupOperationType.RESTORE);

        List<BackupReportCounter> details = new ArrayList<>();
        BackupSummary summary = new BackupSummary();
        report.setDetails(details);
        report.setSummary(summary);

        int processed = 0;
        int errors = 0;
        int total = 0;
        List<BackupReportCounter> reportListByEntries = new ArrayList<>();

        try {
            Object envelope = objectMapper.readValue(backupJsonInput, getEnvelopeClass());
            if (envelope == null) {
                summary.setTotalEntities(0);
                summary.setProcessed(0);
                summary.setSkipped(0);
                summary.setErrors(1);
                report.setStatus(BackupOperationStatus.FAILED);
                return report;
            }

            ImportEnvelopeDescriptor descriptor = buildImportDescriptor(envelope);

            // 1. Простые импорты (без связей)
            for (SimpleImportTask<?, ?, ?> rawTask : descriptor.simpleImportTasks()) {
                ImportResult r = executeSimpleImport(rawTask);
                processed += r.processed();
                errors += r.errors();
                total += r.total();
                reportListByEntries.add(r.reportByEntity());
            }

            // 2. Восстановление связей
            for (RelationRestoreTask relationTask : descriptor.relationTasks()) {
                try {
                    relationTask.relationRestorer().run();
                } catch (Exception ex) {
                    errors++;
                    log.error("[BACKUP_importData] Ошибка при восстановлении связей '{}'",
                            relationTask.name(), ex);
                }
            }

            summary.setTotalEntities(total);
            summary.setProcessed(processed);
            summary.setSkipped(total - processed);
            summary.setErrors(errors);
            report.setDetails(reportListByEntries);
            report.setStatus(errors == 0
                    ? BackupOperationStatus.SUCCESS
                    : BackupOperationStatus.COMPLETED_WITH_WARNINGS);

            return report;

        } catch (IOException e) {
            log.error("[BACKUP_importData] Ошибка чтения backup.json", e);

            summary.setTotalEntities(0);
            summary.setProcessed(0);
            summary.setSkipped(0);
            summary.setErrors(1);
            report.setStatus(BackupOperationStatus.FAILED);
            return report;
        }
    }

    /**
     * Выполнить "простой импорт" одной сущности (без связей).
     */
    @SuppressWarnings("unchecked")
    private <DTO, E, ID> ImportResult executeSimpleImport(SimpleImportTask<?, ?, ?> rawTask) {
        SimpleImportTask<DTO, E, ID> task = (SimpleImportTask<DTO, E, ID>) rawTask;

        BackupReportCounter report = new BackupReportCounter();

        int processed = 0;
        int errors = 0;
        int total = 0;

//        int idi = 0;

        ID oldId = null;
        List<BackupReportEntry> entries = new ArrayList<>();
        for (DTO dto : task.dtos()) {
            total++;
//            idi++;

            try {
                oldId = task.originalIdExtractor().apply(dto);

                E entity = task.mapper().fromDto(dto);
                task.nullifyId().accept(entity);
//                if (idi == 2) throw new RuntimeException("Сам выбросил ошибку"); //генерируем ошибку сами
                E saved = task.repository().save(entity);

                if (oldId != null) {
                    task.oldToNewMap().put(oldId, saved);
                }

                processed++;
            } catch (Exception ex) {
                errors++;
                BackupReportEntry entry = new BackupReportEntry();
                entry.setOriginalId(String.valueOf(oldId));
                entry.setStatus(BackupEntityStatus.ERROR);
                entry.setReasonCode(null);
                entry.setReasonMessage(ex.getMessage());
                entries.add(entry);
                log.error("[BACKUP_importData] Ошибка при импорте {} dto={}",
                        task.entityName(), dto, ex);
            }
        }

        report.setEntityName(task.entityName());
        report.setTotal(total);
        report.setExported(processed);
        report.setSkipped(errors);
        report.setDetails(entries);

        return new ImportResult(processed, errors, total, report);
    }

    // ---------- DTO/record-описатели для импорта ----------

    /**
     * Описывает всё, что нужно для импортирования одной "версии" бэкапа:
     * набор простых импортов + набор восстановителей связей.
     */
    public record ImportEnvelopeDescriptor(
            List<SimpleImportTask<?, ?, ?>> simpleImportTasks,
            List<RelationRestoreTask> relationTasks
    ) {}

    /**
     * "Простой импорт" сущности без восстановления связей.
     * Описывает, как из списка DTO получить новые сущности и построить карту oldId → новая сущность.
     *
     * @param entityName          человекочитаемое имя сущности (для логирования/отладки)
     * @param dtos                список DTO из бэкапа, которые нужно импортировать (наконкретную сущность, например dtos на пользователей)
     * @param mapper              маппер DTO → Entity (как из DTO собрать сущность)
     * @param repository          репозиторий, через который сущности будут сохранены
     * @param nullifyId           операция, обнуляющая id у сущности перед сохранением (чтобы создалась новая запись)
     * @param originalIdExtractor функция, извлекающая "старый" идентификатор из DTO (id из бэкапа)
     * @param oldToNewMap         мапа, куда будет записано соответствие старый id → новая сохранённая сущность
     */
    public record SimpleImportTask<DTO, E, ID>(
            String entityName,
            List<DTO> dtos,
            BackupMapper<DTO, E> mapper,
            JpaRepository<E, ?> repository,
            java.util.function.Consumer<E> nullifyId,
            Function<DTO, ID> originalIdExtractor,
            Map<ID, E> oldToNewMap
    ) {}

    /**
     * Восстановление связей – просто Runnable с человеком-читаемым именем.
     */
    public record RelationRestoreTask(
            String name,
            Runnable relationRestorer
    ) {}

    /**
     * Результат исполнения одного SimpleImportTask.
     */
    public record ImportResult(
            int processed,
            int errors,
            int total,
            BackupReportCounter reportByEntity
    ) {}

    /**
     * Простейший интерфейс маппера DTO -> Entity.
     */
    protected interface BackupMapper<DTO, E> {
        E fromDto(DTO dto);
    }

    // -------------- HELPERS --------------------

    //Восстановление связей ManyToOne, OneToOne: связь child → parent
    protected <DTO, CHILD, PARENT, CHILD_ID, PARENT_ID> RelationRestoreTask relationChildToParent(
            String name,
            List<DTO> dtos,
            ChildIdExtractor<DTO, CHILD_ID> childOriginalIdExtractor,
            ParentIdExtractor<DTO, PARENT_ID> parentOriginalIdExtractor,
            Map<CHILD_ID, CHILD> childByOldId,
            Map<PARENT_ID, PARENT> parentByOldId,
            SetParentOnChild<CHILD, PARENT> setParentOnChild,
            JpaRepository<CHILD, ?> childRepository
    ) {
        return new RelationRestoreTask(
                name,
                () -> {
                    for (DTO dto : dtos) {
                        CHILD_ID oldChildId = childOriginalIdExtractor.apply(dto);
                        if (oldChildId == null) continue;

                        CHILD child = childByOldId.get(oldChildId);
                        if (child == null) continue;

                        PARENT_ID oldParentId = parentOriginalIdExtractor.apply(dto);
                        if (oldParentId == null) continue;

                        PARENT parent = parentByOldId.get(oldParentId);
                        if (parent == null) continue;

                        setParentOnChild.accept(child, parent);
                        childRepository.save(child);
                    }
                }
        );
    }

    /**
     * Хелпер для восстановления двусторонней (bidirectional) связи ManyToMany между двумя сущностями.
     * <p>
     * Пример: User ↔ FileData или User ↔ Location.
     * <br>
     * На вход получает список DTO, старые идентификаторы обеих сторон связи, мапы oldId → сущность
     * и функции, которые выставляют ссылки друг на друга.
     *
     * @param name                 человекочитаемое имя задачи (для логов/отладки)
     * @param dtos                 список DTO бэкапа, в которых хранятся старые id обеих сторон связи
     * @param aOriginalIdExtractor функция, извлекающая старый id стороны A из DTO
     * @param bOriginalIdExtractor функция, извлекающая старый id стороны B из DTO
     * @param aByOldId             мапа соответствий старый id A → новая сущность A
     * @param bByOldId             мапа соответствий старый id B → новая сущность B
     * @param setBOnA              операция, которая устанавливает ссылку на B внутри A (A::setB)
     * @param setAOnB              операция, которая устанавливает ссылку на A внутри B (B::setA)
     * @param aRepository          репозиторий для сохранения сущности A после установки связи
     * @param bRepository          репозиторий для сохранения сущности B (может быть null, если сохранять B не нужно)
     * @param <DTO>                тип DTO, из которых восстанавливаются связи
     * @param <A>                  тип первой сущности (сторона A связи)
     * @param <B>                  тип второй сущности (сторона B связи)
     * @param <A_ID>               тип старого идентификатора для A
     * @param <B_ID>               тип старого идентификатора для B
     */
    protected <DTO, A, B, A_ID, B_ID> RelationRestoreTask relationBidirectional(
            String name,
            List<DTO> dtos,
            SideAIdExtractor<DTO, A_ID> aOriginalIdExtractor,
            SideBIdExtractor<DTO, B_ID> bOriginalIdExtractor,
            Map<A_ID, A> aByOldId,
            Map<B_ID, B> bByOldId,
            SetBOnA<A, B> setBOnA,
            SetAOnB<A, B> setAOnB,
            JpaRepository<A, ?> aRepository,
            @Nullable JpaRepository<B, ?> bRepository
    ) {
        return new RelationRestoreTask(
                name,
                () -> {
                    for (DTO dto : dtos) {
                        A_ID oldAId = aOriginalIdExtractor.apply(dto);
                        if (oldAId == null) continue;

                        A a = aByOldId.get(oldAId);
                        if (a == null) continue;

                        B_ID oldBId = bOriginalIdExtractor.apply(dto);
                        if (oldBId == null) continue;

                        B b = bByOldId.get(oldBId);
                        if (b == null) continue;

                        setBOnA.accept(a, b);
                        setAOnB.accept(b, a);

                        aRepository.save(a);
                        if (bRepository != null) {
                            bRepository.save(b);
                        }
                    }
                }
        );
    }

    /**
     * Преобразует сущность в DTO для бэкапа.
     *
     * @param <T> тип сущности
     * @param <D> тип DTO
     */
    @FunctionalInterface
    protected interface ExportMapper<T, D> extends Function<T, D> {}

    /**
     * Извлекает строковый идентификатор из сущности при экспорте.
     *
     * @param <T> тип сущности
     */
    @FunctionalInterface
    protected interface ExportIdExtractor<T> extends Function<T, String> {}

    /**
     * Извлекает старый идентификатор дочерней сущности из DTO.
     *
     * @param <DTO> тип объекта бэкап‑DTO
     * @param <ID>  тип идентификатора (старого id)
     */
    @FunctionalInterface
    protected interface ChildIdExtractor<DTO, ID> extends Function<DTO, ID> {}

    /**
     * Извлекает старый идентификатор родительской сущности из DTO.
     *
     * @param <DTO> тип объекта бэкап‑DTO
     * @param <ID>  тип идентификатора (старого id)
     */
    @FunctionalInterface
    protected interface ParentIdExtractor<DTO, ID> extends Function<DTO, ID> {}

    /**
     * Устанавливает ссылку на родительскую сущность внутри дочерней.
     *
     * @param <CHILD>  тип дочерней сущности
     * @param <PARENT> тип родительской сущности
     */
    @FunctionalInterface
    protected interface SetParentOnChild<CHILD, PARENT> extends BiConsumer<CHILD, PARENT> {}


    /**
     * Извлекает старый идентификатор первой стороны двусторонней связи (A) из DTO.
     * Например: User в паре User ↔ FileData.
     *
     * @param <DTO> тип объекта бэкап‑DTO
     * @param <ID>  тип идентификатора (старого id A)
     */
    @FunctionalInterface
    protected interface SideAIdExtractor<DTO, ID> extends Function<DTO, ID> {}

    /**
     * Извлекает старый идентификатор второй стороны двусторонней связи (B) из DTO.
     * Например: FileData или Location в паре User ↔ FileData / User ↔ Location.
     *
     * @param <DTO> тип объекта бэкап‑DTO
     * @param <ID>  тип идентификатора (старого id B)
     */
    @FunctionalInterface
    protected interface SideBIdExtractor<DTO, ID> extends Function<DTO, ID> {}

    /**
     * Устанавливает ссылку на сущность B внутри сущности A
     * в двусторонней связи OneToOne.
     *
     * @param <A> тип первой стороны связи
     * @param <B> тип второй стороны связи
     */
    @FunctionalInterface
    protected interface SetBOnA<A, B> extends BiConsumer<A, B> {}

    /**
     * Устанавливает ссылку на сущность A внутри сущности B
     * в двусторонней связи OneToOne.
     *
     * @param <A> тип первой стороны связи
     * @param <B> тип второй стороны связи
     */
    @FunctionalInterface
    protected interface SetAOnB<A, B> extends BiConsumer<B, A> {}
}
