package fr.mossaab.security.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    /* ---------- репозитории ---------- */
    private final UserRepository         userRepo;
    private final FileDataRepository     fileRepo;
    private final RefreshTokenRepository tokenRepo;

    /*
     * Если добавляете новые сущности, не забудьте внедрить нужные
     * репозитории и раскомментировать логику в snapshot()/restore().
     */

    private final ObjectMapper mapper;

    /* ---------- файл ---------- */
    @Value("${app.backup-path}")
    private String backupPath;           // в application.properties
    private Path   json;                 // …/backup.json

    @PostConstruct
    void init() throws Exception {
        Files.createDirectories(Paths.get(backupPath));
        json = Paths.get(backupPath, "backup.json");
        log.info("⤵  Backup file: {}", json.toAbsolutePath());
    }

    /* ---------- СБОР ДАННЫХ ---------- */
    private BackupPayload snapshot() {
        return BackupPayload.builder()
                .users(userRepo.findAll())
                .files(fileRepo.findAll())
                .refreshTokens(tokenRepo.findAll())
                .build();
    }

    /* ---------- СОХРАНЯЕМ ---------- */
    @Transactional(readOnly = true)
    public void save() {
        try {
            String jsonText = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(snapshot());

            Files.writeString(
                    json,
                    jsonText,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.SYNC     // flush на диск
            );
            log.info("✅ Backup written to {}", json.getFileName());

        } catch (Exception e) {
            log.error("❌ Backup write failed", e);
        }
    }

    /* ---------- ВОССТАНАВЛИВАЕМ ---------- */
    @Transactional
    public boolean restoreIfExists() {
        if (Files.notExists(json)) {
            log.warn("🛈 Backup file not found: {}", json);
            return false;
        }
        try {
            BackupPayload p = mapper.readValue(json.toFile(), BackupPayload.class);

            /* чистим таблицы ― порядок важен из-за FK */
            tokenRepo.deleteAllInBatch();
            fileRepo.deleteAllInBatch();
            userRepo.deleteAllInBatch();

            /* пишем обратно */
            userRepo.saveAll(p.getUsers());
            fileRepo.saveAll(p.getFiles());
            tokenRepo.saveAll(p.getRefreshTokens());

            log.info("✅ Restored from {}", json.getFileName());
            return true;
        } catch (Exception e) {
            log.error("❌ Restore failed", e);
            return false;
        }
    }
}
