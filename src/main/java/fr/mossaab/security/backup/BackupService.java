package fr.mossaab.security.backup;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.RefreshTokenRepository;
import fr.mossaab.security.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Основной сервис бэкапа/восстановления.
 * Только User, FileData, RefreshToken.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    /* ───────────── Repositories ───────────── */

    private final UserRepository userRepo;
    private final FileDataRepository fileRepo;
    private final RefreshTokenRepository tokenRepo;

    /* ───────────── Infra ───────────── */

    private final EntityManager entityManager;
    private final ObjectMapper mapper;

    /* ───────────── Config ───────────── */

    @Value("${app.backup-path}")
    private String backupPath;

    private Path json;

    /* ───────────── State ───────────── */

    private final ReentrantLock ioLock = new ReentrantLock();
    private final AtomicBoolean alreadyRestored = new AtomicBoolean(false);

    /* ───────────── Init ───────────── */

    @PostConstruct
    void init() throws Exception {
        Files.createDirectories(Paths.get(backupPath));
        json = Paths.get(backupPath, "backup.json");
        log.info("📁 Backup file location: {}", json.toAbsolutePath());
    }

    /* ===================== SAVE ===================== */

    @Transactional(readOnly = true)
    public void save() {
        ioLock.lock();
        try {
            log.info("💾 Starting backup...");

            // Достаём из БД
            List<User> users = userRepo.findAll();
            List<FileData> files = fileRepo.findAll();
            List<RefreshToken> tokens = tokenRepo.findAll();

            // Делаем копии без проблемных циклов
            List<User> usersCopy = new ArrayList<>();
            for (User u : users) {
                usersCopy.add(copyUserForBackup(u));
            }

            List<FileData> filesCopy = new ArrayList<>();
            for (FileData f : files) {
                filesCopy.add(copyFileForBackup(f));
            }

            List<RefreshToken> tokensCopy = new ArrayList<>();
            for (RefreshToken t : tokens) {
                tokensCopy.add(copyTokenForBackup(t));
            }

            BackupPayload payload = BackupPayload.builder()
                    .users(usersCopy)
                    .files(filesCopy)
                    .refreshTokens(tokensCopy)
                    .build();

            String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            // Бэкап предыдущей версии
            Path prev = json.resolveSibling("backup.json.prev");
            if (Files.exists(json)) {
                try {
                    Files.copy(json, prev, StandardCopyOption.REPLACE_EXISTING);
                    log.info("↪️ Previous backup saved as {}", prev.getFileName());
                } catch (Exception ex) {
                    log.warn("⚠️ Cannot create backup.json.prev: {}", ex.getMessage());
                }
            }

            Files.writeString(
                    json,
                    jsonStr,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            log.info("✅ Backup successfully written: users={}, files={}, tokens={}",
                    usersCopy.size(), filesCopy.size(), tokensCopy.size());
        } catch (Exception e) {
            log.error("❌ Backup failed", e);
        } finally {
            ioLock.unlock();
        }
    }

    /* ==================== RESTORE ==================== */

    @Transactional
    public boolean restoreIfExists() {
        if (alreadyRestored.get()) {
            return true;
        }
        if (Files.notExists(json)) {
            log.warn("ℹ️ Backup file not found: {}", json.toAbsolutePath());
            return false;
        }

        ioLock.lock();
        try {
            Path prev = json.resolveSibling("backup.json.prev");
            Optional<BackupPayload> opt = readBackupPayload(json,
                    Files.exists(prev) ? prev : null);

            if (opt.isEmpty()) {
                log.error("🛑 Cannot read backup.json nor backup.json.prev");
                return false;
            }

            BackupPayload p = opt.get();

            // 1. Чистим таблицы
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            Stream.of(
                    "refresh_token",
                    "file_data",
                    "_user"
            ).forEach(tbl ->
                    entityManager.createNativeQuery("TRUNCATE TABLE " + tbl).executeUpdate()
            );
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

            int usersOk = 0;
            int filesOk = 0;
            int tokensOk = 0;

            // oldId -> new User
            Map<Long, User> oldId2user = new HashMap<>();

            /* ────────── 2. Users ────────── */
            for (User fromBackup : safeList(p.getUsers())) {
                try {
                    Long oldId = fromBackup.getId();

                    // защита от случайных связей в json
                    fromBackup.setFileData(null);
                    fromBackup.setRefreshTokens(null);

                    // обнуляем id, чтобы создать новые
                    fromBackup.setId(null);

                    User saved = userRepo.save(fromBackup);
                    usersOk++;

                    if (oldId != null) {
                        oldId2user.put(oldId, saved);
                    }
                } catch (Exception ex) {
                    log.warn("⚠️ User restore skipped: {}", ex.getMessage());
                }
            }

            /* ────────── 3. FileData ────────── */
            for (FileData fromBackup : safeList(p.getFiles())) {
                try {
                    Long oldUserId = (fromBackup.getUser() != null)
                            ? fromBackup.getUser().getId()
                            : null;

                    fromBackup.setId(null);

                    User newUser = (oldUserId != null) ? oldId2user.get(oldUserId) : null;
                    fromBackup.setUser(newUser);

                    FileData savedFile = fileRepo.save(fromBackup);
                    filesOk++;

                    // поддерживаем двустороннюю связь
                    if (newUser != null) {
                        newUser.setFileData(savedFile);
                        userRepo.save(newUser);
                    }
                } catch (Exception ex) {
                    log.warn("⚠️ FileData restore skipped: {}", ex.getMessage());
                }
            }

            /* ────────── 4. RefreshTokens ────────── */
            for (RefreshToken fromBackup : safeList(p.getRefreshTokens())) {
                try {
                    Long oldUserId = (fromBackup.getUser() != null)
                            ? fromBackup.getUser().getId()
                            : null;

                    fromBackup.setId(null);

                    User newUser = (oldUserId != null) ? oldId2user.get(oldUserId) : null;
                    if (newUser == null) {
                        log.warn("⚠️ RefreshToken skipped – user not found (oldUserId={})", oldUserId);
                        continue;
                    }

                    fromBackup.setUser(newUser);
                    tokenRepo.save(fromBackup);
                    tokensOk++;
                } catch (Exception ex) {
                    log.warn("⚠️ RefreshToken restore skipped: {}", ex.getMessage());
                }
            }

            alreadyRestored.set(true);
            log.info("✅ Restore finished: users={}, files={}, tokens={}",
                    usersOk, filesOk, tokensOk);
            return true;

        } catch (Exception e) {
            log.error("❌ Restore failed", e);
            return false;
        } finally {
            ioLock.unlock();
        }
    }

    /* ==================== HELPERS ==================== */

    private Optional<BackupPayload> readBackupPayload(Path primary, Path fallback) {
        try {
            log.info("📖 Reading backup from {}", primary.toAbsolutePath());
            return Optional.of(mapper.readValue(primary.toFile(), BackupPayload.class));
        } catch (Exception e1) {
            log.error("❌ Can't parse {}: {}", primary, e1.getMessage());
            if (fallback != null && Files.exists(fallback)) {
                try {
                    log.warn("↩️ Trying fallback {}", fallback.toAbsolutePath());
                    return Optional.of(mapper.readValue(fallback.toFile(), BackupPayload.class));
                } catch (Exception e2) {
                    log.error("❌ Can't parse fallback {}: {}", fallback, e2.getMessage());
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Безопасно возвращает неизменяемый пустой список, если исходный == null.
     */
    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : List.of();
    }

    private User copyUserForBackup(User src) {
        if (src == null) return null;
        return User.builder()
                .id(src.getId())
                .temporarySecondsBalance(src.getTemporarySecondsBalance())
                .nickname(src.getNickname())
                .email(src.getEmail())
                .tempEmail(src.getTempEmail())
                .password(src.getPassword())
                .phone(src.getPhone())
                .phoneActivationCode(src.getPhoneActivationCode())
                .phoneVerified(src.getPhoneVerified())
                .activationCode(src.getActivationCode())
                .role(src.getRole())
                // fileData / refreshTokens не тащим – они идут отдельно
                .build();
    }

    private FileData copyFileForBackup(FileData src) {
        if (src == null) return null;
        return FileData.builder()
                .id(src.getId())
                .name(src.getName())
                .type(src.getType())
                .filePath(src.getFilePath())
                // только ссылка по userId
                .user(src.getUser() != null
                        ? User.builder().id(src.getUser().getId()).build()
                        : null)
                .build();
    }

    private RefreshToken copyTokenForBackup(RefreshToken src) {
        if (src == null) return null;
        return RefreshToken.builder()
                .id(src.getId())
                .token(src.getToken())
                .expiryDate(src.getExpiryDate())
                .revoked(src.isRevoked())
                .user(src.getUser() != null
                        ? User.builder().id(src.getUser().getId()).build()
                        : null)
                .build();
    }

    public boolean isAlreadyRestored() {
        return alreadyRestored.get();
    }
}
