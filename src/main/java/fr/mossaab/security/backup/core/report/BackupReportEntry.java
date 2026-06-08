package fr.mossaab.security.backup.core.report;

import fr.mossaab.security.backup.core.enums.BackupEntityStatus;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Детальная запись отчёта по отдельной сущности при бэкапе/восстановлении.")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupReportEntry {

    @Schema(description = "Статус обработки сущности (успешно, пропущено, ошибка и т.п.).",
            example = "SKIPPED")
    private BackupEntityStatus status;

    @Schema(description = "Идентификатор сущности в исходном бэкапе.",
            example = "12345")
    private String originalId;

    @Schema(description = "Код причины (машиночитаемый), например UNIQUE_CONSTRAINT_VIOLATION.",
            example = "UNIQUE_CONSTRAINT_VIOLATION")
    private String reasonCode;

    @Schema(description = "Человекочитаемое описание причины без чувствительных данных.",
            example = "User with this email already exists")
    private String reasonMessage;
}
