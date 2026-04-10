package fr.mossaab.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecretsCheckRunner implements CommandLineRunner {

    // Список секретов для PROD-профиля
    private static final List<String> PROD_SECRETS = List.of(
            "VK_CLIENT_ID", "VK_CLIENT_SECRET",
            "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET",
            "YANDEX_CLIENT_ID", "YANDEX_CLIENT_SECRET",
            "SMS_SERVICE_PASSWORD", "SMS_SERVICE_API_ID",
            "DATABASE_MYSQL_USERNAME", "DATABASE_MYSQL_PASSWORD",
            "SECURITY_JWT_SECRET_KEY",
            "GMAIL_SERVICE_USERNAME", "GMAIL_SERVICE_PASSWORD",
            "APP_UPLOAD_PATH", "APP_BACKUP_PATH"
    );

    // Список секретов для LOCAL-профиля
    private static final List<String> LOCAL_SECRETS = List.of(
            "LOCAL_VK_CLIENT_ID", "LOCAL_VK_CLIENT_SECRET",
            "LOCAL_GOOGLE_CLIENT_ID", "LOCAL_GOOGLE_CLIENT_SECRET",
            "LOCAL_YANDEX_CLIENT_ID", "LOCAL_YANDEX_CLIENT_SECRET",
            "LOCAL_DATABASE_MYSQL_USERNAME", "LOCAL_DATABASE_MYSQL_PASSWORD",
            "LOCAL_GMAIL_SERVICE_USERNAME", "LOCAL_GMAIL_SERVICE_PASSWORD",
            "LOCAL_APP_UPLOAD_PATH", "LOCAL_APP_BACKUP_PATH"
    );

    private final Environment env;

    @Override
    public void run(String... args) {
        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        log.info("🔎 Проверка секретов. Активный профиль: {}", profile);

        switch (profile) {
            case "prod" -> checkSecretsForProfile("prod", PROD_SECRETS, true);
            case "local" -> checkSecretsForProfile("local", LOCAL_SECRETS, false);
            default ->
                    log.warn("⚠ Профиль '{}' не поддержан в SecretsCheckRunner. Проверка секретов пропущена.", profile);
        }
    }

    /**
     * Проверка набора секретов для конкретного профиля
     *
     * @param profileName     имя профиля (для логов)
     * @param requiredSecrets список имён переменных окружения
     * @param failOnMissing   true — падать при отсутствии, false — только логировать warning
     */
    private void checkSecretsForProfile(String profileName,
                                        List<String> requiredSecrets,
                                        boolean failOnMissing) {
        boolean allSecretsPresent = true;

        for (String secretName : requiredSecrets) {
            boolean isPresent = env.getProperty(secretName) != null;
            if (!isPresent) {
                log.warn("❌ [{}] Требуемый секрет не найден: {}", profileName, secretName);
                allSecretsPresent = false;
            } else {
                log.debug("✅ [{}] Секрет найден: {}", profileName, secretName);
            }
        }

        if (failOnMissing && !allSecretsPresent) {
            log.error("💥 [{}] Приложение не может запуститься: необходимые секреты отсутствуют!", profileName);
            throw new IllegalStateException("Необходимые секреты отсутствуют для профиля " + profileName);
        }

        if (allSecretsPresent) {
            log.info("🎉 [{}] Все необходимые секреты присутствуют.", profileName);
        } else {
            log.warn("⚠ [{}] Не все секреты заданы. Приложение продолжит работу (профиль dev/local).", profileName);
        }
    }
}

