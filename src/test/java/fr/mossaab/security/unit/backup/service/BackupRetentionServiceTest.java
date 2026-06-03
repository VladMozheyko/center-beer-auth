package fr.mossaab.security.unit.backup.service;


import fr.mossaab.security.backup.core.config.BackupRetentionProperties;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitTests - BackupRetentionService")
class BackupRetentionServiceTest {

    @Mock
    private BackupFileService backupFileService;

    @Mock
    private BackupRetentionProperties backupRetentionProperties;

    @InjectMocks
    private BackupRetentionService backupRetentionService;

    @Nested
    @DisplayName("applyRetention(DAILY)")
    class DailyRetentionTests {

        @Test
        @DisplayName("Удаляет DAILY-бэкапы, созданные до cutoff-даты")
        void applyRetention_Daily_ShouldDeleteBackupsOlderThanCutoff() throws Exception {
            when(backupRetentionProperties.getDailyDays()).thenReturn(3);

            String oldBackup = "daily-old.zip";
            String newBackup = "daily-new.zip";

            when(backupFileService.list(BackupTier.DAILY))
                    .thenReturn(List.of(oldBackup, newBackup));

            // now и cutoff будут вычисляться внутри сервиса
            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate cutoffDate = now.minusDays(3);
            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

            // oldBackup: сильно старше cutoff (минус 10 дней)
            Instant oldCreated = cutoffInstant.minusSeconds(10L * 24 * 3600);
            when(backupFileService.getCreationTime(oldBackup, BackupTier.DAILY))
                    .thenReturn(FileTime.from(oldCreated));

            // newBackup: явно моложе cutoff (плюс 1 день)
            Instant newCreated = cutoffInstant.plusSeconds(24 * 3600);
            when(backupFileService.getCreationTime(newBackup, BackupTier.DAILY))
                    .thenReturn(FileTime.from(newCreated));

            backupRetentionService.applyRetention(BackupTier.DAILY);

            verify(backupFileService).delete(oldBackup, BackupTier.DAILY);
            verify(backupFileService, never()).delete(eq(newBackup), any());
        }

        @Test
        @DisplayName("Ничего не удаляет, если список файлов пустой")
        void applyRetention_Daily_ShouldDoNothingWhenNoBackups() throws Exception {
            when(backupRetentionProperties.getDailyDays()).thenReturn(7);
            when(backupFileService.list(BackupTier.DAILY)).thenReturn(List.of());

            backupRetentionService.applyRetention(BackupTier.DAILY);

            verify(backupFileService, never()).delete(anyString(), any());
        }

        @Test
        @DisplayName("Пропускает файл, если не удалось прочитать время создания (IOException)")
        void applyRetention_Daily_ShouldSkipFileWhenGetCreationTimeFails() throws Exception {
            when(backupRetentionProperties.getDailyDays()).thenReturn(7);

            String badFile = "bad.zip";
            String goodFile = "good.zip";

            when(backupFileService.list(BackupTier.DAILY))
                    .thenReturn(List.of(badFile, goodFile));

            // для badFile getCreationTime бросает IOException
            when(backupFileService.getCreationTime(badFile, BackupTier.DAILY))
                    .thenThrow(new IOException("test IO"));

            // goodFile старше cutoff -> должен быть удалён
            // пусть cutoffInstant будет сейчас - 7 дней, а goodFile ещё старше
            Instant cutoffInstant = LocalDate.now(ZoneOffset.UTC)
                    .minusDays(7)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            Instant goodCreated = cutoffInstant.minusSeconds(3600);

            when(backupFileService.getCreationTime(goodFile, BackupTier.DAILY))
                    .thenReturn(FileTime.from(goodCreated));

            backupRetentionService.applyRetention(BackupTier.DAILY);

            // badFile не удаляется из-за ошибки чтения
            verify(backupFileService, never()).delete(eq(badFile), any());
            // goodFile должен быть удалён
            verify(backupFileService, times(1)).delete(goodFile, BackupTier.DAILY);
        }

        @Test
        @DisplayName("Не выбрасывает исключение наружу, если list(...) кидает IOException")
        void applyRetention_Daily_ShouldCatchIOExceptionFromList() throws Exception {
            when(backupRetentionProperties.getDailyDays()).thenReturn(7);

            when(backupFileService.list(BackupTier.DAILY))
                    .thenThrow(new IOException("list failed"));

            // метод не должен кидать исключение
            backupRetentionService.applyRetention(BackupTier.DAILY);

            verify(backupFileService, never()).delete(anyString(), any());
        }
    }

    @Nested
    @DisplayName("applyRetention(WEEKLY / MONTHLY / SEMI_ANNUAL / ANNUAL)")
    class OtherTiersRetentionTests {

        @Test
        @DisplayName("Корректно вычисляет cutoff для WEEKLY и удаляет старые бэкапы")
        void applyRetention_Weekly_ShouldDeleteBackupsOlderThanCutoff() throws Exception {
            when(backupRetentionProperties.getWeeklyWeeks()).thenReturn(2);

            String oldFile = "weekly-old.zip";
            String newFile = "weekly-new.zip";

            when(backupFileService.list(BackupTier.WEEKLY))
                    .thenReturn(List.of(oldFile, newFile));

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate cutoffDate = now.minusWeeks(2);
            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

            Instant oldCreated = cutoffInstant.minusSeconds(3600); // до cutoff
            Instant newCreated = cutoffInstant.plusSeconds(24 * 3600); // после cutoff

            when(backupFileService.getCreationTime(oldFile, BackupTier.WEEKLY))
                    .thenReturn(FileTime.from(oldCreated));
            when(backupFileService.getCreationTime(newFile, BackupTier.WEEKLY))
                    .thenReturn(FileTime.from(newCreated));

            backupRetentionService.applyRetention(BackupTier.WEEKLY);

            verify(backupFileService).delete(oldFile, BackupTier.WEEKLY);
            verify(backupFileService, never()).delete(eq(newFile), any());
        }

        @Test
        @DisplayName("Корректно вычисляет cutoff для MONTHLY и удаляет старые бэкапы")
        void applyRetention_Monthly_ShouldDeleteBackupsOlderThanCutoff() throws Exception {
            when(backupRetentionProperties.getMonthlyMonths()).thenReturn(3);

            String oldFile = "monthly-old.zip";
            String newFile = "monthly-new.zip";

            when(backupFileService.list(BackupTier.MONTHLY))
                    .thenReturn(List.of(oldFile, newFile));

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate cutoffDate = now.minusMonths(3);
            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

            Instant oldCreated = cutoffInstant.minusSeconds(3600);
            Instant newCreated = cutoffInstant.plusSeconds(24 * 3600);

            when(backupFileService.getCreationTime(oldFile, BackupTier.MONTHLY))
                    .thenReturn(FileTime.from(oldCreated));
            when(backupFileService.getCreationTime(newFile, BackupTier.MONTHLY))
                    .thenReturn(FileTime.from(newCreated));

            backupRetentionService.applyRetention(BackupTier.MONTHLY);

            verify(backupFileService).delete(oldFile, BackupTier.MONTHLY);
            verify(backupFileService, never()).delete(eq(newFile), any());
        }

        @Test
        @DisplayName("Корректно вычисляет cutoff для SEMI_ANNUAL (years * 6 месяцев)")
        void applyRetention_SemiAnnual_ShouldUseSemiAnnualYearsTimesSixMonths() throws Exception {
            // допустим, semiAnnualYears = 1 => 6 месяцев
            when(backupRetentionProperties.getSemiAnnualYears()).thenReturn(1);

            String oldFile = "semi-old.zip";
            String newFile = "semi-new.zip";

            when(backupFileService.list(BackupTier.SEMI_ANNUAL))
                    .thenReturn(List.of(oldFile, newFile));

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate cutoffDate = now.minusMonths(1 * 6L);
            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

            Instant oldCreated = cutoffInstant.minusSeconds(3600);
            Instant newCreated = cutoffInstant.plusSeconds(24 * 3600);

            when(backupFileService.getCreationTime(oldFile, BackupTier.SEMI_ANNUAL))
                    .thenReturn(FileTime.from(oldCreated));
            when(backupFileService.getCreationTime(newFile, BackupTier.SEMI_ANNUAL))
                    .thenReturn(FileTime.from(newCreated));

            backupRetentionService.applyRetention(BackupTier.SEMI_ANNUAL);

            verify(backupFileService).delete(oldFile, BackupTier.SEMI_ANNUAL);
            verify(backupFileService, never()).delete(eq(newFile), any());
        }

        @Test
        @DisplayName("Корректно вычисляет cutoff для ANNUAL и удаляет старые бэкапы")
        void applyRetention_Annual_ShouldDeleteBackupsOlderThanCutoff() throws Exception {
            when(backupRetentionProperties.getAnnualYears()).thenReturn(5);

            String oldFile = "annual-old.zip";
            String newFile = "annual-new.zip";

            when(backupFileService.list(BackupTier.ANNUAL))
                    .thenReturn(List.of(oldFile, newFile));

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate cutoffDate = now.minusYears(5);
            Instant cutoffInstant = cutoffDate.atStartOfDay().toInstant(ZoneOffset.UTC);

            Instant oldCreated = cutoffInstant.minusSeconds(3600);
            Instant newCreated = cutoffInstant.plusSeconds(24 * 3600);

            when(backupFileService.getCreationTime(oldFile, BackupTier.ANNUAL))
                    .thenReturn(FileTime.from(oldCreated));
            when(backupFileService.getCreationTime(newFile, BackupTier.ANNUAL))
                    .thenReturn(FileTime.from(newCreated));

            backupRetentionService.applyRetention(BackupTier.ANNUAL);

            verify(backupFileService).delete(oldFile, BackupTier.ANNUAL);
            verify(backupFileService, never()).delete(eq(newFile), any());
        }
    }
}