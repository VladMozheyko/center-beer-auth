package fr.mossaab.security.unit.backup.service;

import fr.mossaab.security.backup.core.dto.response.BackupReportSummaryResponse;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.report.BackupSummary;
import fr.mossaab.security.backup.core.service.BackupFileService;
import fr.mossaab.security.backup.core.service.impl.BackupHistoryService;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UnitTests - BackupHistoryService")
@ExtendWith(MockitoExtension.class)
class BackupHistoryServiceTest {

    @Mock private BackupFileService backupFileService;
    @Mock private BackupArchiveReader archiveReader;

    @InjectMocks private BackupHistoryService historyService;

    @Test
    @DisplayName("listHistory: без фильтров возвращает все summary и сортирует по startedAt по убыванию")
    void listHistory_shouldReturnSummariesForAllTiers_whenNoFilters() throws Exception {
        // для простоты тестируем только один tier, остальные вернём пустыми
        when(backupFileService.list(BackupTier.DAILY))
                .thenReturn(List.of("b1.zip", "b2.zip"));
        when(backupFileService.list(BackupTier.WEEKLY))
                .thenReturn(List.of());
        when(backupFileService.list(BackupTier.MONTHLY))
                .thenReturn(List.of());
        when(backupFileService.list(BackupTier.SEMI_ANNUAL))
                .thenReturn(List.of());
        when(backupFileService.list(BackupTier.ANNUAL))
                .thenReturn(List.of());

        // входные потоки нам в этом тесте не важны — можно отдавать "пустой" InputStream
        when(backupFileService.load(anyString(), any()))
                .thenAnswer(invocation ->
                        new ByteArrayInputStream(new byte[0])
                );

        BackupReport report1 = createReport(
                BackupOperationStatus.SUCCESS,
                Instant.parse("2024-01-02T10:00:00Z"),
                Instant.parse("2024-01-02T10:05:00Z"),
                10, 10, 0
        );
        BackupReport report2 = createReport(
                BackupOperationStatus.FAILED,
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:03:00Z"),
                5, 3, 2
        );

        when(archiveReader.readReport(any(InputStream.class)))
                .thenReturn(report1, report2);


        List<BackupReportSummaryResponse> result =
                historyService.listHistory(null, null);

        assertThat(result).hasSize(2);

        // проверяем сортировку по startedAt (по убыванию)
        assertThat(result.get(0).startedAt())
                .isEqualTo(Instant.parse("2024-01-02T10:00:00Z"));
        assertThat(result.get(1).startedAt())
                .isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));

        // проверяем пару полей
        BackupReportSummaryResponse first = result.get(0);
        assertThat(first.status()).isEqualTo(BackupOperationStatus.SUCCESS);
        assertThat(first.totalEntities()).isEqualTo(10);
        assertThat(first.processed()).isEqualTo(10);
        assertThat(first.errorCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("listHistory: применяет фильтры по tier и status")
    void listHistory_shouldApplyTierAndStatusFilters() throws Exception {
        // given
        when(backupFileService.list(BackupTier.WEEKLY))
                .thenReturn(List.of("ok.zip", "failed.zip"));

        when(backupFileService.load(anyString(), eq(BackupTier.WEEKLY)))
                .thenAnswer(invocation ->
                        new ByteArrayInputStream(new byte[0])
                );

        BackupReport okReport = createReport(
                BackupOperationStatus.SUCCESS,
                Instant.parse("2024-01-03T10:00:00Z"),
                Instant.parse("2024-01-03T10:01:00Z"),
                10, 10, 0
        );
        BackupReport failedReport = createReport(
                BackupOperationStatus.FAILED,
                Instant.parse("2024-01-02T10:00:00Z"),
                Instant.parse("2024-01-02T10:01:00Z"),
                10, 5, 5
        );

        when(archiveReader.readReport(any(InputStream.class)))
                .thenReturn(okReport, failedReport);

        //  фильтруем только WEEKLY и только SUCCESS
        List<BackupReportSummaryResponse> result =
                historyService.listHistory(BackupTier.WEEKLY, BackupOperationStatus.SUCCESS);


        assertThat(result).hasSize(1);
        BackupReportSummaryResponse summary = result.get(0);
        assertThat(summary.tier()).isEqualTo(BackupTier.WEEKLY);
        assertThat(summary.status()).isEqualTo(BackupOperationStatus.SUCCESS);
    }

    @Test
    @DisplayName("listHistory: пропускает tier, если list() кидает IOException")
    void listHistory_shouldSkipFilesWhenListThrowsIOException() throws Exception {
        // эмулируем ошибку при получении списка файлов для DAILY
        when(backupFileService.list(BackupTier.DAILY))
                .thenThrow(new IOException("disk error"));

        // для остальных уровней вернём пустые списки
        when(backupFileService.list(BackupTier.WEEKLY)).thenReturn(List.of());
        when(backupFileService.list(BackupTier.MONTHLY)).thenReturn(List.of());
        when(backupFileService.list(BackupTier.SEMI_ANNUAL)).thenReturn(List.of());
        when(backupFileService.list(BackupTier.ANNUAL)).thenReturn(List.of());

        List<BackupReportSummaryResponse> result =
                historyService.listHistory(null, null);

        assertThat(result).isEmpty();
        verify(backupFileService, times(1)).list(BackupTier.DAILY);
    }

    @Test
    @DisplayName("listHistory: пропускает файл, если report.json отсутствует (readReport вернул null)")
    void listHistory_shouldSkipFileWhenReportIsNull() throws Exception {

        when(backupFileService.list(BackupTier.DAILY))
                .thenReturn(List.of("no-report.zip"));

        when(backupFileService.load(anyString(), eq(BackupTier.DAILY)))
                .thenAnswer(invocation ->
                        new ByteArrayInputStream(new byte[0])
                );

        // archiveReader.readReport вернёт null – имитируем отсутствие report.json
        when(archiveReader.readReport(any(InputStream.class)))
                .thenReturn(null);

        List<BackupReportSummaryResponse> result =
                historyService.listHistory(BackupTier.DAILY, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listHistory: пропускает файл, если load() кидает IOException")
    void listHistory_shouldSkipFileWhenLoadThrowsIOException() throws Exception {
        when(backupFileService.list(BackupTier.DAILY))
                .thenReturn(List.of("broken.zip"));

        when(backupFileService.load("broken.zip", BackupTier.DAILY))
                .thenThrow(new IOException("read error"));

        List<BackupReportSummaryResponse> result =
                historyService.listHistory(BackupTier.DAILY, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("loadReport: возвращает отчёт, если report.json прочитан успешно")
    void loadReport_shouldReturnReportWhenPresent() throws Exception {
        String fileName = "ok.zip";
        BackupTier tier = BackupTier.DAILY;

        when(backupFileService.load(fileName, tier))
                .thenAnswer(invocation ->
                        new ByteArrayInputStream(new byte[0])
                );

        BackupReport report = createReport(
                BackupOperationStatus.SUCCESS,
                Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:01:00Z"),
                5, 5, 0
        );

        when(archiveReader.readReport(any(InputStream.class)))
                .thenReturn(report);

        BackupReport loaded = historyService.loadReport(fileName, tier);


        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo(BackupOperationStatus.SUCCESS);
    }

    @Test
    @DisplayName("loadReport: кидает IllegalStateException, если report.json отсутствует (readReport вернул null)")
    void loadReport_shouldThrowWhenReportIsNull() throws Exception {
        String fileName = "no-report.zip";
        BackupTier tier = BackupTier.DAILY;

        when(backupFileService.load(fileName, tier))
                .thenAnswer(invocation ->
                        new ByteArrayInputStream(new byte[0])
                );

        when(archiveReader.readReport(any(InputStream.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> historyService.loadReport(fileName, tier))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Не удалось прочитать report.json");
    }

    @Test
    @DisplayName("loadReport: кидает IllegalStateException, если при чтении архива произошёл IOException")
    void loadReport_shouldThrowWhenIOExceptionOccurs() throws Exception {
        String fileName = "broken.zip";
        BackupTier tier = BackupTier.DAILY;

        when(backupFileService.load(fileName, tier))
                .thenThrow(new IOException("IO error"));

        assertThatThrownBy(() -> historyService.loadReport(fileName, tier))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Не удалось прочитать report.json");
    }

    // ==== вспомогательные методы ====
    private BackupReport createReport(
            BackupOperationStatus status,
            Instant startedAt,
            Instant finishedAt,
            Integer total,
            Integer processed,
            Integer errors
    ) {
        BackupReport report = new BackupReport();
        report.setStatus(status);
        report.setStartedAt(startedAt);
        report.setFinishedAt(finishedAt);

        BackupSummary summary = new BackupSummary();
        summary.setTotalEntities(total);
        summary.setProcessed(processed);
        summary.setErrors(errors);
        report.setSummary(summary);

        return report;
    }
}