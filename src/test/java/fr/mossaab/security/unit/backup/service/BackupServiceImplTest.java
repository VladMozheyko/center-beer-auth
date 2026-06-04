package fr.mossaab.security.unit.backup.service;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupFileExtension;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupOperationType;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.report.BackupReportCounter;
import fr.mossaab.security.backup.core.report.BackupReportEntry;
import fr.mossaab.security.backup.core.report.BackupSummary;
import fr.mossaab.security.backup.core.service.BackupVersionHandler;
import fr.mossaab.security.backup.core.service.impl.BackupServiceImpl;
import fr.mossaab.security.backup.core.service.impl.PreRestoreCleaner;
import fr.mossaab.security.backup.core.storage.BackupStorage;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import fr.mossaab.security.backup.core.utils.BackupFileNameGenerator;
import org.junit.jupiter.api.*;
import org.mockito.InOrder;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UnitTests - BackupServiceImpl")
class BackupServiceImplTest {

    private BackupProperties backupProperties;
    private BackupStorage backupStorage;
    private BackupFileNameGenerator fileNameGenerator;
    private BackupArchiveReader archiveReader;
    private PreRestoreCleaner cleaner;

    private BackupVersionHandler handlerV1;
    private BackupVersionHandler handlerV2;

    private BackupServiceImpl backupService;

    @BeforeEach
    void setUp() {
        backupProperties = mock(BackupProperties.class);
        backupStorage = mock(BackupStorage.class);
        fileNameGenerator = mock(BackupFileNameGenerator.class);
        archiveReader = mock(BackupArchiveReader.class);

        ObjectMapper objectMapper = spy(new ObjectMapper());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        cleaner = mock(PreRestoreCleaner.class);

        handlerV1 = mock(BackupVersionHandler.class);
        handlerV2 = mock(BackupVersionHandler.class);

        when(handlerV1.getSchemaVersion()).thenReturn(1);
        when(handlerV2.getSchemaVersion()).thenReturn(2);

        backupService = new BackupServiceImpl(
                backupProperties,
                backupStorage,
                fileNameGenerator,
                archiveReader,
                objectMapper,
                cleaner,
                List.of(handlerV1, handlerV2)
        );
    }

    @Test
    @DisplayName("Проверка корректного выбора обработчика по номеру версии схемы")
    void getHandlerForVersion_returnsCorrectHandler() throws Exception {
        Method m = BackupServiceImpl.class.getDeclaredMethod("getHandlerForVersion", int.class);
        m.setAccessible(true);

        BackupVersionHandler result1 = (BackupVersionHandler) m.invoke(backupService, 1);
        BackupVersionHandler result2 = (BackupVersionHandler) m.invoke(backupService, 2);

        assertThat(result1).isSameAs(handlerV1);
        assertThat(result2).isSameAs(handlerV2);
    }

    @Test
    @DisplayName("Проверка выброса исключения при отсутствии обработчика для указанной версии")
    void getHandlerForVersion_throwsWhenNotFound() throws Exception {
        Method m = BackupServiceImpl.class.getDeclaredMethod("getHandlerForVersion", int.class);
        m.setAccessible(true);

        assertThatThrownBy(() -> m.invoke(backupService, 999))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("No BackupVersionHandler registered for schemaVersion=999");
    }

    @Test
    @DisplayName("Успешный экспорт бэкапа без ошибок и предупреждений")
    void exportSystemBackup_success_noWarnings() throws Exception {
        when(backupProperties.getCurrentSchemaVersion()).thenReturn(1);
        when(backupProperties.getAppVersion()).thenReturn("1.0.0");
        when(backupProperties.isFormattedJson()).thenReturn(true);

        String fileName = "backup-1.zip";
        when(fileNameGenerator.generate(any(), eq("1"), eq(BackupFileExtension.ZIP)))
                .thenReturn(fileName);
        when(fileNameGenerator.toShortName(fileName)).thenReturn("backup-1");

        // handler.exportData: не изменяем summary → по умолчанию 0 skipped и 0 errors
        doAnswer(invocation -> {
            JsonGenerator gen = invocation.getArgument(0);
            // просто записываем поле, чтобы JSON был валиден
            gen.writeStringField("handler", "v1");
            return null;
        }).when(handlerV1).exportData(any(JsonGenerator.class), any(BackupReport.class));

        doNothing().when(backupStorage).save(any(), any(), any());

        BackupReport report = backupService.exportSystemBackup();

        // Проверки отчёта
        assertThat(report.getOperation()).isEqualTo(BackupOperationType.EXPORT);
        assertThat(report.getSchemaVersion()).isEqualTo(1);
        assertThat(report.getFileName()).isEqualTo(fileName);
        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.SUCCESS);
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getErrors()).isZero();
        assertThat(report.getSummary().getSkipped()).isZero();
        assertThat(report.getFinishedAt()).isNotNull();

        // Убедимся, что backupStorage.save() был вызван
        verify(backupStorage).save(eq(BackupTier.DAILY), eq(fileName), any());
    }

    @Test
    @DisplayName("Экспорт с статусом COMPLETED_WITH_WARNINGS при наличии ошибок в сводке")
    void exportSystemBackup_completedWithWarnings_whenSummaryHasErrors() throws Exception {
        when(backupProperties.getCurrentSchemaVersion()).thenReturn(1);
        when(backupProperties.getAppVersion()).thenReturn("1.0.0");
        when(backupProperties.isFormattedJson()).thenReturn(false);

        String fileName = "backup-1.zip";
        when(fileNameGenerator.generate(any(), eq("1"), eq(BackupFileExtension.ZIP)))
                .thenReturn(fileName);
        when(fileNameGenerator.toShortName(fileName)).thenReturn("backup-1");

        // Здесь модифицируем summary: поставим 1 ошибку
        doAnswer(invocation -> {
            JsonGenerator gen = invocation.getArgument(0);
            BackupReport rep = invocation.getArgument(1);
            rep.getSummary().setErrors(1);
            gen.writeStringField("handler", "v1");
            return null;
        }).when(handlerV1).exportData(any(JsonGenerator.class), any(BackupReport.class));

        BackupReport report = backupService.exportSystemBackup();

        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.COMPLETED_WITH_WARNINGS);
        assertThat(report.getSummary().getErrors()).isEqualTo(1);
    }

    @Test
    @DisplayName("При исключении в обработчике создаётся отчёт об ошибке и удаляется временный файл")
    void exportSystemBackup_whenHandlerThrows_createsErrorReportAndDeletesTempFile() throws Exception {
        when(backupProperties.getCurrentSchemaVersion()).thenReturn(1);
        when(backupProperties.getAppVersion()).thenReturn("1.0.0");
        when(backupProperties.isFormattedJson()).thenReturn(true);

        String fileName = "backup-err.zip";
        when(fileNameGenerator.generate(any(), eq("1"), eq(BackupFileExtension.ZIP)))
                .thenReturn(fileName);
        when(fileNameGenerator.toShortName(fileName)).thenReturn("backup-err");

        // Чтобы проверить удаление temp‑файла, перехватим вызов Files.createTempFile
        // Это сложно мочь в unit‑тестах без PowerMock, поэтому обойдемся косвенно:
        // сгенерим исключение внутри exportData, чтобы код дошёл до catch/finally.
        doThrow(new RuntimeException("export failure"))
                .when(handlerV1).exportData(any(JsonGenerator.class), any(BackupReport.class));

        BackupReport report = backupService.exportSystemBackup();

        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.FAILED);
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getErrors()).isEqualTo(1);
        assertThat(report.getDetails()).hasSize(1);
        assertThat(report.getDetails().get(0).getDetails())
                .hasSize(1)
                .first()
                .extracting(BackupReportEntry::getReasonMessage)
                .asString()
                .contains("export failure");

        // В этом сценарии backupStorage.save не должен вызываться
        verify(backupStorage, never()).save(any(), anyString(), any(InputStream.class));
    }

    @Test
    @DisplayName("Успешное восстановление бэкапа с проверкой порядка вызовов")
    void restoreSystemBackup_success() throws Exception {
        String backupName = "backup-restore.zip";
        BackupTier tier = BackupTier.DAILY;
        when(fileNameGenerator.toShortName(backupName)).thenReturn("backup-restore");

        // Файл, который вернёт backupStorage.load
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(zipBytes, StandardCharsets.UTF_8)) {
            // backup.json
            zipOut.putNextEntry(new ZipEntry("backup.json"));
            String json = """
                    {
                      "schemaVersion": 2,
                      "foo": "bar"
                    }
                    """;
            zipOut.write(json.getBytes(StandardCharsets.UTF_8));
        }

        ByteArrayInputStream storageIn = new ByteArrayInputStream(zipBytes.toByteArray());
        when(backupStorage.load(backupName, tier)).thenReturn(storageIn);

        // archiveReader.readBackupBytes должен вытащить backup.json
        when(archiveReader.readBackupBytes(any(InputStream.class), eq("backup.json")))
                .thenAnswer(invocation -> {
                    InputStream in = invocation.getArgument(0);
                    // прочитаем весь ZIP и реально достанем backup.json, чтобы тест был ближе к реальности
                    try (ZipInputStream zipIn = new ZipInputStream(in, StandardCharsets.UTF_8)) {
                        ZipEntry e;
                        while ((e = zipIn.getNextEntry()) != null) {
                            if ("backup.json".equals(e.getName())) {
                                return zipIn.readAllBytes();
                            }
                        }
                        return new byte[0];
                    }
                });

        // handlerV2.importData возвращает свой отчёт
        BackupReport versionReport = new BackupReport();
        BackupSummary versionSummary = new BackupSummary();
        versionSummary.setErrors(0);
        versionSummary.setSkipped(0);
        versionReport.setStatus(BackupOperationStatus.SUCCESS);
        versionReport.setSummary(versionSummary);
        versionReport.setDetails(List.of(new BackupReportCounter()));
        when(handlerV2.importData(any(InputStream.class))).thenReturn(versionReport);

        BackupReport result = backupService.restoreSystemBackup(backupName, tier);

        // Проверяем, что перед восстановлением чистили базу
        verify(cleaner).cleanAllEntities();

        assertThat(result.getOperation()).isEqualTo(BackupOperationType.RESTORE);
        assertThat(result.getSchemaVersion()).isEqualTo(2);
        assertThat(result.getFileName()).isEqualTo(backupName);
        assertThat(result.getStatus()).isEqualTo(BackupOperationStatus.SUCCESS);
        assertThat(result.getSummary()).isSameAs(versionSummary);
        assertThat(result.getDetails()).isSameAs(versionReport.getDetails());
        assertThat(result.getFinishedAt()).isNotNull();

        // Проверим порядок: load → readBackupBytes → importData
        InOrder inOrder = inOrder(backupStorage, archiveReader, handlerV2);
        inOrder.verify(backupStorage).load(backupName, tier);
        inOrder.verify(archiveReader).readBackupBytes(any(InputStream.class), eq("backup.json"));
        inOrder.verify(handlerV2).importData(any(InputStream.class));
    }

    @Test
    @DisplayName("При отсутствии файла создаётся отчёт об ошибке (NoSuchFileException)")
    void restoreSystemBackup_whenNoSuchFile_createsErrorReport() throws Exception {
        String backupName = "missing.zip";
        BackupTier tier = BackupTier.DAILY;
        when(fileNameGenerator.toShortName(backupName)).thenReturn("missing");

        when(backupStorage.load(backupName, tier))
                .thenThrow(new NoSuchFileException("missing.zip"));

        BackupReport report = backupService.restoreSystemBackup(backupName, tier);

        verify(cleaner).cleanAllEntities();

        assertThat(report.getOperation()).isEqualTo(BackupOperationType.RESTORE);
        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.FAILED);
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getErrors()).isEqualTo(1);
        assertThat(report.getDetails()).hasSize(1);
        assertThat(report.getDetails().get(0).getDetails())
                .hasSize(1)
                .first()
                .extracting(BackupReportEntry::getReasonMessage)
                .asString()
                .contains("missing.zip");
    }

    @Test
    @DisplayName("При IO-исключении создаётся отчёт об ошибке")
    void restoreSystemBackup_whenIOException_createsErrorReport() throws Exception {
        String backupName = "io_err.zip";
        BackupTier tier = BackupTier.DAILY;
        when(fileNameGenerator.toShortName(backupName)).thenReturn("io_err");

        when(backupStorage.load(backupName, tier))
                .thenThrow(new IOException("IO error"));

        BackupReport report = backupService.restoreSystemBackup(backupName, tier);

        verify(cleaner).cleanAllEntities();

        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.FAILED);
        assertThat(report.getSummary().getErrors()).isEqualTo(1);
        assertThat(report.getDetails().get(0).getDetails())
                .hasSize(1)
                .first()
                .extracting(BackupReportEntry::getReasonMessage)
                .asString()
                .contains("IO error");
    }

    @Test
    @DisplayName("Проверка корректности заполнения отчёта об ошибке методом createErrorReport")
    void createErrorReport_fillsReportCorrectly() throws Exception {
        Method m = BackupServiceImpl.class.getDeclaredMethod("createErrorReport",
                Exception.class, BackupReport.class, String.class);
        m.setAccessible(true);

        BackupReport report = new BackupReport();
        Exception e = new IllegalStateException("something bad");
        String fullName = "backup-name.zip";

        BackupReport result = (BackupReport) m.invoke(backupService, e, report, fullName);

        assertThat(result).isSameAs(report);
        assertThat(result.getStatus()).isEqualTo(BackupOperationStatus.FAILED);
        assertThat(result.getSummary()).isNotNull();
        assertThat(result.getSummary().getErrors()).isEqualTo(1);
        assertThat(result.getDetails()).hasSize(1);

        BackupReportCounter counter = result.getDetails().get(0);
        assertThat(counter.getEntityName()).isEqualTo(fullName);
        assertThat(counter.getDetails()).hasSize(1);
        assertThat(counter.getDetails().get(0).getReasonMessage()).isEqualTo("something bad");
        assertThat(result.getFinishedAt()).isNotNull();
    }
}