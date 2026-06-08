package fr.mossaab.security.integration.backup.service.impl;


import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.service.impl.BackupServiceImpl;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Location;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.integration.AbstractIntegrationTest;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.LocationRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Интеграционный E2E‑тест сервиса резервного копирования.
 * <p>
 * Сценарий:
 * <ul>
 *   <li>Создаёт в БД пользователя вместе с локацией, аватаром (FileData)
 *       и социальной учётной записью (UserSocialAccount);</li>
 *   <li>Выполняет экспорт полного состояния системы через {@link BackupServiceImpl};</li>
 *   <li>Очищает все связанные таблицы, имитируя потерю данных;</li>
 *   <li>Выполняет восстановление из только что созданного бэкапа;</li>
 *   <li>Проверяет, что все данные и связи (User ↔ Location, User ↔ FileData, User ↔ UserSocialAccount)
 *       корректно восстановлены.</li>
 * </ul>
 */
@SpringBootTest
@DisplayName("E2E‑тест: экспорт и восстановление полной пользовательской структуры из бэкапа")
// чтобы контекст можно было «портить» очисткой БД между тестами
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "backup.file-system.base-path=${java.io.tmpdir}/test-backup")
class BackupServiceExportRestoreE2ETest extends AbstractIntegrationTest {

    @TempDir
    Path sharedTempDir;

    @Autowired
    private BackupProperties backupProperties;

    @Autowired
    private BackupServiceImpl backupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private FileDataRepository fileDataRepository;

    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;

    @BeforeEach
    void cleanDatabase() {
        userSocialAccountRepository.deleteAll();
        fileDataRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        backupProperties.getFileSystem().setBasePath(sharedTempDir.toString());
    }

    @Test
    @DisplayName("Экспорт + очистка БД + восстановление: проверка целостности User, Location, FileData и UserSocialAccount")
    void exportAndRestore_shouldRoundtripDatabaseState_withAllRelations() {
        // given: создаём полный набор связанных сущностей

        Location location = Location.builder()
                .latitude(55.751244)
                .longitude(37.618423)
                .country("Russia")
                .city("Moscow")
                .build();

        User user = User.builder()
                .temporarySecondsBalance(120)
                .nickname("backup-test-nickname")
                .email("backup-test@example.com")
                .password("encoded-password")
                .phone("+79990000000")
                .phoneVerified(true)
                .createdAt(LocalDateTime.now())
                .role(Role.USER)
                .location(location)
                .build();

        // social account
        UserSocialAccount social = UserSocialAccount.builder()
                .provider(OAuthProvider.GOOGLE)
                .externalId("google-12345")
                .socialEmail("social@example.com")
                .user(user)
                .build();

        user.setSocialAccounts(Set.of(social));
        user = userRepository.save(user);

        // fileData
        FileData fileData = FileData.builder()
                .name("avatar.png")
                .type("image/png")
                .filePath("/tmp/avatar.png")
                .user(user)
                .build();
        fileDataRepository.save(fileData);

        user.setFileData(fileData);
        userRepository.save(user);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getLocation()).isNotNull();
        assertThat(user.getFileData()).isNotNull();
        assertThat(user.getSocialAccounts()).hasSize(1);

        // when: экспорт (создание бэкапа)
        BackupReport exportReport = backupService.exportSystemBackup();
        String backupName = exportReport.getFileName();

        assertThat(exportReport.getStatus())
                .isIn(BackupOperationStatus.SUCCESS, BackupOperationStatus.COMPLETED_WITH_WARNINGS);
        assertThat(backupName).isNotBlank();

        // эмулируем «потерю» данных уже после экспорта:
        // можно вручную, но restore сам вызывает cleaner.cleanAllEntities(),
        // всё равно удалим, чтобы убедиться, что данные берутся только из бэкапа.
        userSocialAccountRepository.deleteAll();
        fileDataRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        assertThat(userRepository.findAll()).isEmpty();
        assertThat(locationRepository.findAll()).isEmpty();
        assertThat(fileDataRepository.findAll()).isEmpty();
        assertThat(userSocialAccountRepository.findAll()).isEmpty();

        // then: восстановление из только что созданного бэкапа
        BackupReport restoreReport = backupService.restoreSystemBackup(backupName, BackupTier.DAILY);

        assertThat(restoreReport.getStatus())
                .as("Статус восстановления должен быть успешным или с предупреждениями")
                .isIn(BackupOperationStatus.SUCCESS, BackupOperationStatus.COMPLETED_WITH_WARNINGS);
        assertThat(restoreReport.getFileName()).isEqualTo(backupName);

        // и теперь проверяем, что всё вернулось в БД
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);

        User restoredUser = users.get(0);
        assertThat(restoredUser.getNickname()).isEqualTo("backup-test-nickname");
        assertThat(restoredUser.getEmail()).isEqualTo("backup-test@example.com");
        assertThat(restoredUser.getTemporarySecondsBalance()).isEqualTo(120);
        assertThat(restoredUser.getPhone()).isEqualTo("+79990000000");
        assertThat(restoredUser.getPhoneVerified()).isTrue();
        assertThat(restoredUser.getRole()).isEqualTo(Role.USER);

        // location
        Location restoredLocation = restoredUser.getLocation();
        assertThat(restoredLocation).isNotNull();
        assertThat(restoredLocation.getCountry()).isEqualTo("Russia");
        assertThat(restoredLocation.getCity()).isEqualTo("Moscow");
        assertThat(restoredLocation.getLatitude()).isEqualTo(55.751244);
        assertThat(restoredLocation.getLongitude()).isEqualTo(37.618423);

        // fileData
        FileData restoredFileData = restoredUser.getFileData();
        assertThat(restoredFileData).isNotNull();
        assertThat(restoredFileData.getName()).isEqualTo("avatar.png");
        assertThat(restoredFileData.getType()).isEqualTo("image/png");
        assertThat(restoredFileData.getFilePath()).isEqualTo("/tmp/avatar.png");
        assertThat(restoredFileData.getUser()).isEqualTo(restoredUser);

        // social account
        List<UserSocialAccount> restoredSocialAccounts = userSocialAccountRepository.findAll();
        assertThat(restoredSocialAccounts).hasSize(1);
        UserSocialAccount restoredSocial = restoredSocialAccounts.get(0);
        assertThat(restoredSocial.getUser().getId()).isEqualTo(restoredUser.getId());
        assertThat(restoredSocial.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(restoredSocial.getExternalId()).isEqualTo("google-12345");
        assertThat(restoredSocial.getSocialEmail()).isEqualTo("social@example.com");
    }
}
