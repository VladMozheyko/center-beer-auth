package fr.mossaab.security.backup.core.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Сводная статистика по результатам операции бэкапа/восстановления.")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupSummary {

    @Schema(description = "Общее количество сущностей, найденных в бэкапе.", example = "1500")
    private long totalEntities;

    @Schema(description = "Количество сущностей, успешно обработанных (экспортировано/импортировано).", example = "1450")
    private long processed;

    @Schema(description = "Количество пропущенных сущностей (не обработано по тем или иным причинам).", example = "50")
    private long skipped;

    @Schema(description = "Количество сущностей с ошибками обработки.", example = "3")
    private long errors;
}
