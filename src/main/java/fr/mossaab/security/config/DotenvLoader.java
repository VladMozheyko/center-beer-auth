package fr.mossaab.security.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@Getter
public final class DotenvLoader {

    public static void load() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            System.setProperty(key, value);
            log.debug("Загрузка env переменных из .env: {}", key);
        });
    }
}
