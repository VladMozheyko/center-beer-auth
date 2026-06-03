package fr.mossaab.security.backup.core.dto.response;

import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupOperationType;
import fr.mossaab.security.backup.core.enums.BackupTier;

import java.time.Instant;

public record BackupReportSummaryResponse(
        String fileName,
        BackupTier tier,
        BackupOperationType operation,
        BackupOperationStatus status,
        Integer schemaVersion,
        Instant startedAt,
        Instant finishedAt,
        Long totalEntities,
        Long processed,
        Long errorCount
) { }
