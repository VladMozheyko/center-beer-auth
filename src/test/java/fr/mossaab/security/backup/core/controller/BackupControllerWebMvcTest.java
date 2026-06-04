package fr.mossaab.security.backup.core.controller;

import fr.mossaab.security.backup.core.dto.response.BackupReportSummaryResponse;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupOperationType;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.service.BackupService;
import fr.mossaab.security.backup.core.service.impl.BackupHistoryService;
import fr.mossaab.security.config.TestSecurityConfig;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.UserCreateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvc‑тесты для {@link BackupController}.
 * Проверяются:
 * <ul>
 *   <li>маршруты и HTTP‑методы;</li>
 *   <li>передача параметров в сервисный слой;</li>
 *   <li>сериализация ответа в JSON.</li>
 * </ul>
 */
@WebMvcTest(controllers = BackupController.class)
@ActiveProfiles("test")
@DisplayName("UnitTests - BackupControllerWebMvc")
@Import({TestSecurityConfig.class})
class BackupControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BackupService backupService;

    @MockBean
    private BackupHistoryService backupHistoryService;

    @MockBean
    private UserCreateService userCreateService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /backups/run — вызывает exportSystemBackup и возвращает 200 с BackupReport")
    void run_shouldCallExportSystemBackup_andReturnReport() throws Exception {
        // given
        BackupReport mockReport = BackupReport.builder()
                .fileName("backup-2024-01-01-00-00-00.zip")
                .status(BackupOperationStatus.SUCCESS)
                .startedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .finishedAt(Instant.parse("2024-01-01T00:01:00Z"))
                .build();

        when(backupService.exportSystemBackup()).thenReturn(mockReport);

        // when / then
        mockMvc.perform(post("/backups/run"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // проверяем пару ключевых полей в JSON
                .andExpect(jsonPath("$.fileName").value("backup-2024-01-01-00-00-00.zip"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(backupService).exportSystemBackup();
    }

    @Test
    @DisplayName("POST /backups/restore — передаёт filename и tier в restoreSystemBackup и возвращает BackupReport")
    void restore_shouldCallRestoreSystemBackup_withParams_andReturnReport() throws Exception {
        // given
        String filename = "daily-backup-2024-01-02.zip";
        BackupTier tier = BackupTier.DAILY;

        BackupReport mockReport = BackupReport.builder()
                .fileName(filename)
                .status(BackupOperationStatus.COMPLETED_WITH_WARNINGS)
                .startedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .finishedAt(Instant.parse("2024-01-02T00:02:00Z"))
                .build();

        when(backupService.restoreSystemBackup(filename, tier)).thenReturn(mockReport);

        // when / then
        mockMvc.perform(post("/backups/restore")
                        .param("filename", filename)
                        .param("tier", tier.name()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // несколько полей из JSON
                .andExpect(jsonPath("$.fileName").value(filename))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_WARNINGS"));

        // проверяем, что параметры дошли до сервиса как надо
        verify(backupService).restoreSystemBackup(filename, tier);
    }

    @Test
    @DisplayName("GET /backups/history — передаёт tier и status в listHistory и возвращает список summary‑DTO")
    void listHistory_shouldCallListHistory_withFilters_andReturnList() throws Exception {
        // given
        BackupTier tier = BackupTier.DAILY;
        BackupOperationType type = BackupOperationType.EXPORT;
        BackupOperationStatus status = BackupOperationStatus.SUCCESS;
        Integer schemaVersion = 1;

        BackupReportSummaryResponse summary1 = new BackupReportSummaryResponse(
                "daily-backup-1.zip",
                tier,
                type,
                status,
                schemaVersion,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:01:00Z"),
                0L, 0L, 0L
        );
        BackupReportSummaryResponse summary2 = new BackupReportSummaryResponse(
                "daily-backup-2.zip",
                tier,
                type,
                status,
                schemaVersion,
                Instant.parse("2024-01-02T00:00:00Z"),
                Instant.parse("2024-01-02T00:01:00Z"),
                0L, 0L, 0L
        );

        when(backupHistoryService.listHistory(tier, status))
                .thenReturn(List.of(summary1, summary2));

        // when / then
        mockMvc.perform(get("/backups/history")
                        .param("tier", tier.name())
                        .param("status", status.name()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // список из 2 элементов
                .andExpect(jsonPath("$.length()").value(2))
                // проверяем несколько полей первого элемента
                .andExpect(jsonPath("$[0].fileName").value("daily-backup-1.zip"))
                .andExpect(jsonPath("$[0].tier").value(tier.name()))
                .andExpect(jsonPath("$[0].status").value(status.name()));

        // проверяем аргументы, переданные в сервис
        verify(backupHistoryService).listHistory(tier, status);
    }

    @Test
    @DisplayName("GET /backups/report/{tier} — передаёт tier и fileName в loadReport и возвращает полный BackupReport")
    void getFullReport_shouldCallLoadReport_andReturnReport() throws Exception {
        // given
        BackupTier tier = BackupTier.MONTHLY;
        String fileName = "monthly-backup-2024-01.zip";

        BackupReport mockReport = BackupReport.builder()
                .fileName(fileName)
                .status(BackupOperationStatus.SUCCESS)
                .startedAt(Instant.parse("2024-01-31T23:00:00Z"))
                .finishedAt(Instant.parse("2024-01-31T23:10:00Z"))
                .build();

        when(backupHistoryService.loadReport(fileName, tier)).thenReturn(mockReport);

        // when / then
        mockMvc.perform(get("/backups/report/{tier}", tier.name())
                        .param("fileName", fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fileName").value(fileName))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.startedAt").value("2024-01-31T23:00:00Z"))
                .andExpect(jsonPath("$.finishedAt").value("2024-01-31T23:10:00Z"));

        verify(backupHistoryService).loadReport(fileName, tier);
    }
}