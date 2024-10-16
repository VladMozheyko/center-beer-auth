package fr.mossaab.security.archive;

/*@Service
@RequiredArgsConstructor
public class RestoreService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RestoreService.class);

    @PersistenceContext
    private EntityManager entityManager;
    private static final String BACKUP_FILE_PATH = "/var/www/vuary/user_backup/users_backup.json";

    //private static final String BACKUP_FILE_PATH = "C:/Users/Admin/Desktop/kotitonttu1207/src/main/resources/users_backup.json";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Проверка и восстановление пользователей из резервной копии.");
        if (userRepository.count() == 0) {
            restoreFromBackup();
        } else {
            logger.info("Пользователи уже существуют в базе данных.");
        }
    }

    @Transactional
    public void restoreFromBackup() {
        try {
            logger.info("Чтение данных из файла: " + BACKUP_FILE_PATH);
            List<UserBackupDTO> userDTOs = objectMapper.readValue(new File(BACKUP_FILE_PATH), new TypeReference<>() {});
            logger.info("Найдено " + userDTOs.size() + " пользователей в резервной копии.");

            for (UserBackupDTO userDTO : userDTOs) {
                logger.info("Восстановление пользователя: " + userDTO.getEmail());
                User user = toEntity(userDTO);

                User savedUser = entityManager.merge(user);
                logger.info("Пользователь сохранён/обновлён: " + savedUser.getEmail());

                for (RefreshToken token : user.getRefreshTokens()) {
                    token.setUser(savedUser); // Устанавливаем ссылку на сохраненного пользователя

                    if (token.getId() != null) {
                        if (entityManager.find(RefreshToken.class, token.getId()) != null) {
                            entityManager.merge(token);
                            logger.info("Обновлён токен: " + token.getToken());
                        } else {
                            entityManager.persist(token);
                            logger.info("Создан новый токен: " + token.getToken());
                        }
                    } else {
                        entityManager.persist(token);
                        logger.info("Создан новый токен: " + token.getToken());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла резервной копии", e);
        }
    }

    private User toEntity(UserBackupDTO userDTO) {
        User user = User.builder()
                .firstname(userDTO.getFirstname())
                .lastname(userDTO.getLastname())
                .email(userDTO.getEmail())
                .phoneNumber(userDTO.getPhoneNumber())
                .workerRoles(userDTO.getWorkerRoles())
                .dateOfBirth(userDTO.getDateOfBirth())
                .role(userDTO.getRole())
                .activationCode(userDTO.getActivationCode())
                .build();

        if (userDTO.getFileData() != null) {
            FileData fileData = new FileData();
            fileData.setName(userDTO.getFileData().getName());
            fileData.setType(userDTO.getFileData().getType());
            fileData.setFilePath(userDTO.getFileData().getFilePath());
            fileData.setUser(user);  // Установка обратной связи
            user.setFileData(fileData);
        }

        // Восстановление токенов
        List<RefreshToken> tokens = userDTO.getRefreshTokens().stream()
                .map(dto -> toTokenEntity(dto, user))
                .toList();
        user.setRefreshTokens(tokens);

        return user;
    }

    private RefreshToken toTokenEntity(RefreshTokenDTO tokenDTO, User user) {
        RefreshToken token = RefreshToken.builder()
                .id(tokenDTO.getId())  // предполагаем, что id уже может быть null
                .token(tokenDTO.getToken())
                .expiryDate(tokenDTO.getExpiryDate())
                .revoked(tokenDTO.isRevoked())
                .user(user)  // Установка пользователя
                .build();

        return token;
    }
}

 */