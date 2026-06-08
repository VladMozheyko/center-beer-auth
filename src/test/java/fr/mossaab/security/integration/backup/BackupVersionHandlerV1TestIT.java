package fr.mossaab.security.integration.backup;


import fr.mossaab.security.backup.core.config.BackupProperties;
import fr.mossaab.security.backup.core.enums.BackupFileExtension;
import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.service.BackupService;
import fr.mossaab.security.backup.core.service.impl.PreRestoreCleaner;
import fr.mossaab.security.backup.core.utils.BackupFileNameGenerator;
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
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Slf4j
class BackupVersionHandlerV1TestIT extends AbstractIntegrationTest {

    @Autowired
    private PreRestoreCleaner tablesCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileDataRepository fileDataRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserSocialAccountRepository socialAccountRepository;

    @Autowired
    private BackupService backupService;

    @Autowired
    private BackupProperties properties;

    @MockBean
    private BackupFileNameGenerator fileNameGenerator;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;

    private BackupReport report;


    @BeforeAll
    public static void beforeAll() {
        MODE currentMode = AbstractIntegrationTest.get_MODE();
        log.info("Запуск интеграционных тестов в режиме: {}", currentMode);

        if (currentMode == MODE.H2) {
            log.warn("Тесты будут пропущены: H2 база данных не поддерживает требуемые операции бэкапа/восстановления");
            Assumptions.assumeTrue(false,
                    "Тесты пропущены: режим H2 несовместим с тестируемым сценарием бэкапа/восстановления. " +
                            "Используйте -Dtest.db.mode=testcontainer для запуска с MySQL Testcontainer"
            );
        } else {
            log.info("Режим БД {} поддерживается — продолжаем выполнение тестов", currentMode);
        }
    }


    /**
     * 1. Чистим БД и создаём стартовые данные (2 пользователя + связанные сущности).
     */
    @Test
    @Order(1)
    @Transactional
    @Commit
    void step1_prepareData() {
        // 1.1. Очистить все таблицы
        tablesCleaner.cleanAllEntities();
        assertThat(userRepository.count()).isZero();
        assertThat(fileDataRepository.count()).isZero();
        assertThat(locationRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();

        // 1.2. Создать 2 локации
        Location loc1 = locationRepository.save(
                Location.builder()
                        .city("Москва")
                        .country("Россия")
                        .latitude(55.7558)
                        .longitude(37.6173)
                        .build()
        );

        Location loc2 = locationRepository.save(
                Location.builder()
                        .city("Санкт‑Петербург")
                        .country("Россия")
                        .latitude(59.9343)
                        .longitude(30.3351)
                        .build()
        );

        // 1.3. Создать 2 пользователя
        user1 = userRepository.save(
                User.builder()
                        .nickname("user_one")
                        .password("password1")
                        .email("user1@example.com")
                        .phone("+790000000000")
                        .role(Role.USER)
                        .temporarySecondsBalance(1234)
                        .phoneVerified(true)
                        .activationCode("code-200")
                        .createdAt(LocalDateTime.now())
                        .location(loc1)
                        .build()

        );

        user2 = userRepository.save(
                User.builder()
                        .nickname("alex_dev")
                        .password("securePass2024!")
                        .email("user2@example.com")
                        .phone("+79123456789")
                        .role(Role.ADMIN)
                        .temporarySecondsBalance(5678)
                        .phoneVerified(false)
                        .activationCode("code-305")
                        .createdAt(LocalDateTime.now().minusDays(15))
                        .location(loc2)
                        .build()
        );


        // 1.4. Добавить по файлу каждому пользователю
        FileData file1 = fileDataRepository.save(
                FileData.builder()
                        .name("avatar_user1.jpg")
                        .type("IMAGE")
                        .filePath("/uploads/avatar_user1.jpg")
                        .user(user1)
                        .build()
        );

        FileData file2 = fileDataRepository.save(
                FileData.builder()
                        .name("avatar_user2.jpg")
                        .type("IMAGE")
                        .filePath("/uploads/avatar_user2.jpg")
                        .user(user2)
                        .build()
        );

        user1.setFileData(file1);
        user2.setFileData(file2);


        // 1.5. Добавить соц.аккаунты
        UserSocialAccount sa1 = UserSocialAccount.builder()
                .user(user1)
                .provider(OAuthProvider.GOOGLE)
                .externalId("google_user1")
                .socialEmail("user1@gmail.com")
                .build();

        UserSocialAccount sa2 = UserSocialAccount.builder()
                .user(user2)
                .provider(OAuthProvider.VK)
                .externalId("vk_user2")
                .socialEmail("user2@vk.com")
                .build();

        socialAccountRepository.saveAll(List.of(sa1, sa2));
        user1.setSocialAccounts(new HashSet<>(List.of(sa1)));
        user2.setSocialAccounts(new HashSet<>(List.of(sa2)));
        userRepository.save(user1);
        userRepository.save(user2);

        // 1.6. Проверяем, что всё создалось и связи на месте
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(fileDataRepository.count()).isEqualTo(2);
        assertThat(locationRepository.count()).isEqualTo(2);
        assertThat(socialAccountRepository.count()).isEqualTo(2);

        User loaded1 = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(loaded1.getLocation()).isNotNull();
        assertThat(loaded1.getFileData()).isNotNull();
        assertThat(loaded1.getSocialAccounts()).hasSize(1);

        User loaded2 = userRepository.findById(user2.getId()).orElseThrow();
        assertThat(loaded2.getLocation()).isNotNull();
        assertThat(loaded2.getFileData()).isNotNull();
        assertThat(loaded2.getSocialAccounts()).hasSize(1);
    }

    /**
     * 2. Сделать бэкап и проверить, что он создан.
     */
    @Test
    @Order(2)
    @Transactional
    @Commit
    void step2_createBackup() {
        when(fileNameGenerator.generate(any(Instant.class), any(String.class), any(BackupFileExtension.class)))
                .thenReturn("backup-test.zip");

        report = backupService.exportSystemBackup();

        // Примеры проверок — адаптируй под свой контракт
        assertThat(report).isNotNull();
        assertThat(report.getStatus()).isEqualTo(BackupOperationStatus.SUCCESS);
        assertThat(report.getSummary().getTotalEntities()).isEqualTo(8);
        assertThat(report.getSummary().getProcessed()).isEqualTo(8);

        String basePath = properties.getFileSystem().getBasePath();
        Path path = Path.of(basePath).resolve("DAILY").resolve(report.getFileName());
        File file = path.toFile();
        assertThat(file.exists()).isTrue();
    }

    /**
     * 3. Очистить БД и проверить, что всё пусто.
     */
    @Test
    @Order(3)
    @Transactional
    @Commit
    void step3_cleanDatabase() {
        tablesCleaner.cleanAllEntities();

        assertThat(userRepository.count()).isZero();
        assertThat(fileDataRepository.count()).isZero();
        assertThat(locationRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();

        // При желании проверить seq-таблицы:
        Long userSeq = jdbcTemplate.queryForObject(
                "SELECT next_val FROM _user_seq", Long.class);
        Long fileSeq = jdbcTemplate.queryForObject(
                "SELECT next_val FROM file_data_seq", Long.class);
        log.info("After clean: _user_seq.next_val = {}, file_data_seq.next_val = {}",
                userSeq, fileSeq);
    }

    /**
     * 4. Восстановить данные из бэкапа и проверить, что всё восстановилось.
     */
    @Test
    @Order(4)
    @Transactional
    @Commit
    void step4_restoreAndCheck() {
        // Восстановление
        backupService.restoreSystemBackup("backup-test.zip", BackupTier.DAILY);
        entityManager.flush();
        entityManager.clear();

        // Проверяем, что данные вернулись
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(fileDataRepository.count()).isEqualTo(2);
        assertThat(locationRepository.count()).isEqualTo(2);
        assertThat(socialAccountRepository.count()).isEqualTo(2);

        User u1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        User u2 = userRepository.findByEmail("user2@example.com").orElseThrow();

        // Проверяем связи
        assertThat(u1.getLocation()).isNotNull();
        assertThat(u1.getFileData()).isNotNull();
        assertThat(u1.getSocialAccounts()).hasSize(1);

        assertThat(u2.getLocation()).isNotNull();
        assertThat(u2.getFileData()).isNotNull();
        assertThat(u2.getSocialAccounts()).hasSize(1);

        // Важно: проверить корректность генерации новых id
        // Регистрируем нового пользователя после restore
        Location loc3 = locationRepository.save(
                Location.builder()
                        .city("Казань")
                        .country("Россия")
                        .latitude(55.7964)
                        .longitude(49.1088)
                        .build()
        );


        User newUser = userRepository.save(
                User.builder()
                        .nickname("maria_sm")
                        .password("P@ssw0rd2024")
                        .email("maria.smith@example.org")
                        .phone("+79991234567")
                        .role(Role.ADMIN)
                        .temporarySecondsBalance(2500)
                        .phoneVerified(true)
                        .activationCode("code-401")
                        .createdAt(LocalDateTime.now().minusHours(3))
                        .build()
        );

        log.info("New user id after restore = {}", newUser.getId());

        // Ожидание зависит от твоей политики seq/IDENTITY.
        // Главное — убедиться, что id НЕ уходит в 52, -1 и т.п.,
        // а больше, чем max(id) восстановленных пользователей.
        Long maxId = userRepository.findAll().stream()
                .mapToLong(User::getId)
                .max()
                .orElseThrow();
        assertThat(newUser.getId()).isEqualTo(maxId);
    }
}