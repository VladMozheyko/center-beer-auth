package fr.mossaab.security.integration;

import lombok.Getter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public abstract class AbstractIntegrationTest {

    @Getter
    private static final MODE _MODE = MODE.H2;

    // Не аннотируем @Container, чтобы Testcontainers сам его не трогал.
    // Создаём лениво и только при режиме "tc".
    private static MySQLContainer<?> mysqlContainer;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // DB mode: real | h2 | tc
        // Examples for VM options:
        //   -Dtest.db.mode=real
        //   -Dtest.db.mode=h2
        //   -Dtest.db.mode=tc
        String mode = System.getProperty("test.db.mode", _MODE.getModeName());
        System.out.println("=== TEST DB MODE (System property) = " + mode + " ===");

        switch (mode) {
            case "tc" -> {
                System.out.println("=== Using Testcontainers MySQL ===");
                // лениво создаём контейнер при первом обращении
                if (mysqlContainer == null) {
                    mysqlContainer = new MySQLContainer<>("mysql:8.0")
                            .withDatabaseName("testdb")
                            .withUsername("testuser")
                            .withPassword("testpass");
                }
                if (!mysqlContainer.isRunning()) {
                    mysqlContainer.start();
                }
                registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
                registry.add("spring.datasource.username", mysqlContainer::getUsername);
                registry.add("spring.datasource.password", mysqlContainer::getPassword);
                registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            }
            case "h2" -> {
                System.out.println("=== Using H2 in-memory DB ===");
                registry.add("spring.datasource.url",
                        () -> "jdbc:h2:mem:bm_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
                registry.add("spring.datasource.username", () -> "sa");
                registry.add("spring.datasource.password", () -> "");
                registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
                registry.add("spring.jpa.database-platform",
                        () -> "org.hibernate.dialect.H2Dialect");
                registry.add("spring.jpa.hibernate.ddl-auto",
                        () -> "update");
            }
            case "real" -> // ничего не переопределяем: берём spring.datasource.* из properties
                    System.out.println("=== Using REAL MySQL from application(-test).properties ===");
            default -> throw new IllegalArgumentException(
                    "Unknown test.db.mode: " + mode + " (expected: real|h2|tc)"
            );
        }
    }

    public enum MODE{
        H2("h2"),
        TEST_CONTAINER_MSQL("tc"),
        REAL_MSQL("real");

        private final String modeName;

        MODE(String real) {
            modeName = real;
        }

        public String getModeName() {
            return modeName;
        }
    }
}