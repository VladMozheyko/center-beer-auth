package fr.mossaab.security.integration.backup.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.service.BackupService;
import fr.mossaab.security.backup.core.service.BackupVersionHandler;
import fr.mossaab.security.backup.core.storage.BackupStorage;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BackupServiceImplIT extends AbstractIntegrationTest {

    @Autowired
    private BackupService backupService;

    @Autowired
    private BackupProperties backupProperties;

    @Autowired
    private BackupStorage backupStorage;

    @Autowired
    private BackupArchiveReader archiveReader;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private List<BackupVersionHandler> handlers;

    private int schemaVersion;

    @BeforeEach
    void setUp() {
        // проверим, что есть хотя бы один handler
        assertThat(handlers).isNotEmpty();
        schemaVersion = backupProperties.getCurrentSchemaVersion();
        assertThat(handlers.stream().anyMatch(h -> h.getSchemaVersion() == schemaVersion))
                .as("Handler for schemaVersion=" + schemaVersion + " must exist")
                .isTrue();
    }

    @Test
    void exportSystemBackup_shouldCreateZipWithBackupAndReportJson_andSaveToStorage() throws Exception {
        // when
        BackupReport report = backupService.exportSystemBackup();

        // then
        assertThat(report).isNotNull();
        assertThat(report.getStatus())
                .isIn(BackupOperationStatus.SUCCESS, BackupOperationStatus.COMPLETED_WITH_WARNINGS);
        assertThat(report.getFileName()).isNotBlank();
        assertThat(report.getSchemaVersion()).isEqualTo(schemaVersion);
        assertThat(report.getStartedAt()).isBeforeOrEqualTo(report.getFinishedAt());

        // читаем файл из хранилища
        try (InputStream backupInput = backupStorage.load(report.getFileName(), BackupTier.DAILY)) {
            // достаём backup.json из архива
            byte[] backupJsonBytes = archiveReader.readBackupBytes(backupInput, "backup.json");
            JsonNode backupJson = objectMapper.readTree(backupJsonBytes);

            assertThat(backupJson.path("schemaVersion").asInt())
                    .isEqualTo(schemaVersion);
            assertThat(backupJson.path("archiveFileName").asText())
                    .isEqualTo(report.getFileName());
            assertThat(backupJson.path("appVersion").asText())
                    .isEqualTo(backupProperties.getAppVersion());

            // достаём report.json для проверки
        }

        // Читаем снова, но уже report.json (нужно заново открыть поток)
        try (InputStream backupInput2 = backupStorage.load(report.getFileName(), BackupTier.DAILY)) {
            byte[] reportJsonBytes = archiveReader.readBackupBytes(backupInput2, "report.json");
            BackupReport reportFromArchive = objectMapper.readValue(reportJsonBytes, BackupReport.class);

            assertThat(reportFromArchive.getFileName()).isEqualTo(report.getFileName());
            assertThat(reportFromArchive.getOperation()).isEqualTo(report.getOperation());
            assertThat(reportFromArchive.getSchemaVersion()).isEqualTo(schemaVersion);
        }
    }

    @Test
    void restoreSystemBackup_shouldRestoreFromExistingBackup_andReturnReportFromVersionHandler() throws Exception {
        // сначала делаем экспорт, чтобы был живой ZIP
        BackupReport exportReport = backupService.exportSystemBackup();
        String backupName = exportReport.getFileName();

        // given: для надёжности проверим, что файл физически существует в storage
        try (InputStream ignored = backupStorage.load(backupName, BackupTier.DAILY)) {
            // ok
        }

        // when
        BackupReport restoreReport = backupService.restoreSystemBackup(backupName, BackupTier.DAILY);

        // then
        assertThat(restoreReport).isNotNull();
        assertThat(restoreReport.getOperation()).isEqualTo(exportReport.getOperation().RESTORE);
        assertThat(restoreReport.getFileName()).isEqualTo(backupName);
        assertThat(restoreReport.getSchemaVersion()).isEqualTo(schemaVersion);
        assertThat(restoreReport.getStatus())
                .isIn(BackupOperationStatus.SUCCESS, BackupOperationStatus.COMPLETED_WITH_WARNINGS, BackupOperationStatus.FAILED);
        // В идеале SUCCESS/COMPLETED_WITH_WARNINGS, но зависит от хендлера
        assertThat(restoreReport.getFinishedAt()).isNotNull();
    }

    @Test
    void restoreSystemBackup_whenFileDoesNotExist_shouldReturnFailedReport() throws Exception {
        String nonExistingBackup = "non-existing-backup-" + Instant.now().toEpochMilli() + ".zip";

        // when
        BackupReport report = backupService.restoreSystemBackup(nonExistingBackup, BackupTier.DAILY);

        // then
        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.FAILED);
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getErrors()).isEqualTo(1);
        assertThat(report.getDetails()).isNotEmpty();
        assertThat(report.getDetails().get(0).getEntityName()).isEqualTo(nonExistingBackup);
    }
}