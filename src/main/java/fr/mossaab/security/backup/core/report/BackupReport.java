package fr.mossaab.security.backup.core.report;

import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupOperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Отчёт о выполнении операции бэкапа/восстановления.")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupReport {

    @Schema(description = "Имя файла бэкапа (backup..._.zip)", example = "backup-v1-20260529T182000Z-30d38c5e-6dc0-44aa-9888-4e6b1a3a4d60.zip")
    private String fileName;

    @Schema(description = "Тип операции: экспорт или восстановление.", example = "RESTORE")
    private BackupOperationType operation;

    @Schema(description = "Версия схемы бэкапа.", example = "1")
    private int schemaVersion;

    @Schema(description = "Момент начала операции.", example = "2024-05-10T12:00:00Z")
    private Instant startedAt;

    @Schema(description = "Момент окончания операции.", example = "2024-05-10T12:00:05Z")
    private Instant finishedAt;

    @Schema(description = "Итоговый статус операции.", example = "SUCCESS")
    private BackupOperationStatus status;

    @Schema(description = "Сводная статистика по результатам операции.")
    private BackupSummary summary;

    @Schema(description = "Детализированные результаты по отдельным сущностям/таблицам.")
    @Builder.Default
    private List<BackupReportCounter> details = new ArrayList<>();
}