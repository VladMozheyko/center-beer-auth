package fr.mossaab.security;

import fr.mossaab.security.service.UserCreateService;
import fr.mossaab.security.controller.QuizController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class SecurityApplication {

    private boolean schemaIsEmpty = false;

    @Autowired
    private UserCreateService userCreationService;

    @Autowired
    private QuizController quizController;

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }

    @Transactional
    @PostConstruct
    public void createSamplePresentation() throws IOException {
        userCreationService.createUsers();
        System.out.println("Пример презентации с файлами успешно создан.");

        String result = quizController.updateQuestionsFromCSV();
        System.out.println("Автоматическое обновление вопросов: " + result);

        quizController.reloadQuestionsCacheInternal();
        System.out.println("Кэш вопросов успешно перезагружен.");

        // Настройка параметров MySQL при запуске
        configureMySQLSettings();
        System.out.println("Параметры MySQL успешно настроены.");
    }

    private void configureMySQLSettings() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("SET GLOBAL max_connections = 5000");
            stmt.execute("SET GLOBAL table_open_cache = 4000");
            stmt.execute("SET GLOBAL tmp_table_size = 536870912"); // 512MB
            stmt.execute("SET GLOBAL max_heap_table_size = 536870912"); // 512MB
            stmt.execute("SET GLOBAL innodb_buffer_pool_size = 4294967296"); // 4GB
            stmt.execute("SET GLOBAL innodb_log_buffer_size = 16777216"); // 16MB
            stmt.execute("SET GLOBAL join_buffer_size = 262144"); // 256KB
            stmt.execute("SET GLOBAL sort_buffer_size = 262144"); // 256KB
            stmt.execute("SET GLOBAL max_allowed_packet = 67108864"); // 64MB

            System.out.println("MySQL параметры изменены динамически.");

        } catch (SQLException e) {
            System.err.println("Ошибка при настройке MySQL параметров: " + e.getMessage());
        }
    }
}
