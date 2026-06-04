package fr.mossaab.security.unit.backup.service;

import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.BackupFileService;
import fr.mossaab.security.backup.core.service.impl.BackupPromotionService;
import fr.mossaab.security.backup.core.utils.BackupArchiveReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitTest - BackupPromotionService")
class BackupPromotionServiceTest {

    @Mock
    private BackupFileService backupFileService;

    @Mock
    private BackupArchiveReader backupArchiveReader;

    @InjectMocks
    private BackupPromotionService promotionService;

    @Nested
    @DisplayName("Метод promote(...)")
    class PromoteTests {

        @Test
        @DisplayName("Копирует единственный успешный бэкап в окне периода")
        void promote_ShouldCopySingleSuccessfulBackupWithinPeriod() throws Exception {
            BackupTier from = BackupTier.DAILY;
            BackupTier to = BackupTier.WEEKLY;

            String file = "backup-1.zip";
            when(backupFileService.list(from)).thenReturn(List.of(file));

            Instant now = Instant.now();
            Instant created = now.minusSeconds(3600); // 1 час назад
            when(backupFileService.getCreationTime(eq(file), eq(from)))
                    .thenReturn(FileTime.from(created));

            // isSuccessful -> true
            when(backupFileService.load(eq(file), eq(from)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(true);

            promotionService.promote(from, to, Period.ofDays(1));

            verify(backupFileService).copy(file, from, to);
        }

        @Test
        @DisplayName("Не копирует ни один бэкап, если все неуспешные")
        void promote_ShouldNotCopyIfAllBackupsAreUnsuccessful() throws Exception {
            BackupTier from = BackupTier.DAILY;
            BackupTier to = BackupTier.WEEKLY;

            String file1 = "backup-1.zip";
            String file2 = "backup-2.zip";
            when(backupFileService.list(from)).thenReturn(List.of(file1, file2));

            // оба бэкапа неуспешные
            when(backupFileService.load(anyString(), eq(from)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(false);

            promotionService.promote(from, to, Period.ofDays(1));

            verify(backupFileService, never()).copy(anyString(), any(), any());
        }

        @Test
        @DisplayName("Копирует самый новый успешный бэкап в окне периода")
        void promote_ShouldCopyMostRecentSuccessfulBackupWithinPeriod() throws Exception {
            BackupTier from = BackupTier.DAILY;
            BackupTier to = BackupTier.WEEKLY;

            String oldFile = "backup-old.zip";
            String newFile = "backup-new.zip";
            when(backupFileService.list(from)).thenReturn(List.of(oldFile, newFile));

            Instant now = Instant.now();
            Instant oldCreated = now.minusSeconds(7200); // 2 часа назад
            Instant newCreated = now.minusSeconds(1800); // 30 минут назад

            when(backupFileService.getCreationTime(eq(oldFile), eq(from)))
                    .thenReturn(FileTime.from(oldCreated));
            when(backupFileService.getCreationTime(eq(newFile), eq(from)))
                    .thenReturn(FileTime.from(newCreated));

            // оба бэкапа успешные
            when(backupFileService.load(anyString(), eq(from)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(true);

            promotionService.promote(from, to, Period.ofDays(1));

            // должен быть скопирован только самый новый
            verify(backupFileService, times(1)).copy(eq(newFile), eq(from), eq(to));
            verify(backupFileService, never()).copy(eq(oldFile), any(), any());
        }

        @Test
        @DisplayName("Не копирует бэкапы, если все вне окна периода")
        void promote_ShouldNotCopyWhenAllBackupsAreOutsidePeriod() throws Exception {
            BackupTier from = BackupTier.DAILY;
            BackupTier to = BackupTier.WEEKLY;

            String file = "backup-old.zip";
            when(backupFileService.list(from)).thenReturn(List.of(file));

            Instant now = Instant.now();
            // создаём файл сильно старше, чем период
            Instant created = now.minus(Period.ofDays(10));
            when(backupFileService.getCreationTime(eq(file), eq(from)))
                    .thenReturn(FileTime.from(created));

            when(backupFileService.load(eq(file), eq(from)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(true);

            // период всего 1 день
            promotionService.promote(from, to, Period.ofDays(1));

            verify(backupFileService, never()).copy(anyString(), any(), any());
        }

        @Test
        @DisplayName("Файл, для которого не удалось прочитать атрибуты, исключается из кандидатов")
        void promote_ShouldSkipFileIfCreationTimeThrowsIOException() throws Exception {
            BackupTier from = BackupTier.DAILY;
            BackupTier to = BackupTier.WEEKLY;

            String badFile = "backup-bad.zip";
            String goodFile = "backup-good.zip";

            when(backupFileService.list(from)).thenReturn(List.of(badFile, goodFile));

            // для badFile getCreationTime бросает IOException
            when(backupFileService.getCreationTime(eq(badFile), eq(from)))
                    .thenThrow(new IOException("test IO error"));
            // для goodFile нормальное время
            Instant now = Instant.now();
            when(backupFileService.getCreationTime(eq(goodFile), eq(from)))
                    .thenReturn(FileTime.from(now.minusSeconds(300)));

            // оба считаем успешными
            when(backupFileService.load(anyString(), eq(from)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(true);

            promotionService.promote(from, to, Period.ofDays(1));

            // должен быть скопирован только goodFile
            verify(backupFileService, times(1)).copy(eq(goodFile), eq(from), eq(to));
            verify(backupFileService, never()).copy(eq(badFile), any(), any());
        }

        @Test
        @DisplayName("Исключение в promote логируется и не пробрасывается наружу")
        void promote_ShouldCatchUnexpectedException() throws Exception {
            BackupTier from = BackupTier.DAILY;
            BackupTier to = BackupTier.WEEKLY;

            // провоцируем NPE/RuntimeException через list()
            when(backupFileService.list(from)).thenThrow(new RuntimeException("boom"));

            // метод не должен бросать исключение
            promotionService.promote(from, to, Period.ofDays(1));

            // никаких копирований
            verify(backupFileService, never()).copy(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("Метод isSuccessful(...)")
    class IsSuccessfulTests {

        @Test
        @DisplayName("Возвращает true, если backupArchiveReader пометил бэкап как успешный")
        void isSuccessful_ShouldReturnTrue_WhenArchiveReaderReturnsTrue() throws Exception {
            String fileName = "backup.zip";
            BackupTier tier = BackupTier.DAILY;

            when(backupFileService.load(eq(fileName), eq(tier)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(true);

            boolean result = promotionService.isSuccessful(fileName, tier);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Возвращает false, если backupArchiveReader пометил бэкап как неуспешный")
        void isSuccessful_ShouldReturnFalse_WhenArchiveReaderReturnsFalse() throws Exception {
            String fileName = "backup.zip";
            BackupTier tier = BackupTier.DAILY;

            when(backupFileService.load(eq(fileName), eq(tier)))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(false);

            boolean result = promotionService.isSuccessful(fileName, tier);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Возвращает false, если при чтении отчёта возник IOException")
        void isSuccessful_ShouldReturnFalse_WhenIOExceptionOccurs() throws Exception {
            String fileName = "backup.zip";
            BackupTier tier = BackupTier.DAILY;

            when(backupFileService.load(eq(fileName), eq(tier)))
                    .thenThrow(new IOException("test IO error"));

            boolean result = promotionService.isSuccessful(fileName, tier);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Передаёт InputStream в backupArchiveReader.isBackupSuccessful")
        void isSuccessful_ShouldPassInputStreamToArchiveReader() throws Exception {
            String fileName = "backup.zip";
            BackupTier tier = BackupTier.DAILY;

            ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
            when(backupFileService.load(eq(fileName), eq(tier)))
                    .thenReturn(in);
            when(backupArchiveReader.isBackupSuccessful(any()))
                    .thenReturn(true);

            promotionService.isSuccessful(fileName, tier);

            ArgumentCaptor<java.io.InputStream> captor = ArgumentCaptor.forClass(java.io.InputStream.class);
            verify(backupArchiveReader).isBackupSuccessful(captor.capture());

            assertThat(captor.getValue()).isSameAs(in);
        }
    }
}
