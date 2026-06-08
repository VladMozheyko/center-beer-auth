package fr.mossaab.security.backup.core.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результаты экспорта по одной сущности/таблице.")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupReportCounter {

    @Schema(description = "Имя сущности/раздела бэкапа.", example = "users")
    private String entityName;

    @Schema(description = "Общее количество записей, найденных для экспорта.", example = "1200")
    private long total;

    @Schema(description = "Количество успешно экспортированных записей.", example = "1180")
    private long exported;

    @Schema(description = "Количество пропущенных записей (ошибка маппинга, валидации и т.п.).", example = "20")
    private long skipped;

    @Schema(description = "Детальные записи по пропущенным/проблемным сущностям.")
    @Builder.Default
    private List<BackupReportEntry> details = new ArrayList<>();
}
