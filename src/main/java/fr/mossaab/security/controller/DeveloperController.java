package fr.mossaab.security.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.management.OperatingSystemMXBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(
        name = "Мониторинг системы",
        description = "API-интерфейс для получения технической информации о состоянии сервера, JVM и базы данных. " +
                "Доступ разрешён только администраторам."
)
@RestController
@RequestMapping("/devops")
@SecurityRequirements()
@RequiredArgsConstructor
public class DeveloperController {
    private final DataSource dataSource;
    @Operation(summary = "403 Страница для теста", description = "Возвращает статус 403 — используется для проверки недоступных зон.")
    @GetMapping("/forbidden")
    public ResponseEntity<Void> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @Operation(summary = "Информация о JVM и ОС", description = "Получение информации о версии ОС, архитектуре, количестве процессоров и памяти JVM.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/jvm-os")
    public ResponseEntity<Map<String, Object>> getJvmAndOsStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("os.name", System.getProperty("os.name"));
        map.put("os.arch", System.getProperty("os.arch"));
        map.put("os.version", System.getProperty("os.version"));

        Runtime runtime = Runtime.getRuntime();
        map.put("jvm.availableProcessors", runtime.availableProcessors());
        map.put("jvm.totalMemory", runtime.totalMemory());
        map.put("jvm.freeMemory", runtime.freeMemory());
        map.put("jvm.maxMemory", runtime.maxMemory());

        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Использование CPU и физической памяти", description = "Показывает загрузку CPU и объём свободной/общей физической памяти.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/cpu-memory")
    public ResponseEntity<Map<String, Object>> getCpuAndMemoryStats() {
        Map<String, Object> map = new HashMap<>();
        java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();

        if (baseOsBean instanceof OperatingSystemMXBean osBean) {
            map.put("cpu.processLoad", osBean.getProcessCpuLoad());
            map.put("cpu.systemLoad", osBean.getSystemCpuLoad());
            map.put("memory.physical.free", osBean.getFreePhysicalMemorySize());
            map.put("memory.physical.total", osBean.getTotalPhysicalMemorySize());
        }

        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Статистика Tomcat", description = "Отображает количество текущих и занятых потоков в ThreadPool Tomcat.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/tomcat")
    public ResponseEntity<Map<String, Object>> getTomcatStats() {
        Map<String, Object> map = new HashMap<>();
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName threadPool = new ObjectName("Tomcat:type=ThreadPool,name=\"http-nio-8080\"");

            map.put("tomcat.threads.busy", mBeanServer.getAttribute(threadPool, "currentThreadsBusy"));
            map.put("tomcat.threads.total", mBeanServer.getAttribute(threadPool, "currentThreadCount"));
        } catch (Exception e) {
            map.put("tomcat.error", e.getMessage());
        }

        return ResponseEntity.ok(map);
    }
    @Operation(summary = "MySQL статус и переменные", description = "Показывает полную статистику MySQL: глобальные статусы и переменные.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/mysql")
    public ResponseEntity<Map<String, Object>> getMySqlStats() {
        Map<String, Object> map = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SHOW GLOBAL STATUS")) {
                Map<String, String> status = new HashMap<>();
                while (rs.next()) {
                    status.put(rs.getString(1), rs.getString(2));
                }
                map.put("mysql.status", status);
            }

            try (ResultSet rs = stmt.executeQuery("SHOW GLOBAL VARIABLES")) {
                Map<String, String> variables = new HashMap<>();
                while (rs.next()) {
                    variables.put(rs.getString(1), rs.getString(2));
                }
                map.put("mysql.variables", variables);
            }

        } catch (Exception e) {
            map.put("mysql.error", e.getMessage());
        }

        return ResponseEntity.ok(map);
    }

    @Operation(summary = "Uptime приложения", description = "Показывает, сколько времени работает текущее приложение (в секундах).")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/uptime")
    public ResponseEntity<Map<String, Object>> getUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        return ResponseEntity.ok(Map.of("uptime.ms", uptime, "uptime.sec", uptime / 1000));
    }
    @Operation(summary = "Информация о потоках", description = "Показывает количество активных потоков и подробности по группам.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> getThreadStats() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> map = new HashMap<>();
        map.put("thread.count", threadMXBean.getThreadCount());
        map.put("peak.thread.count", threadMXBean.getPeakThreadCount());
        map.put("daemon.thread.count", threadMXBean.getDaemonThreadCount());
        map.put("total.started.thread.count", threadMXBean.getTotalStartedThreadCount());
        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Переменные окружения", description = "Показывает переменные окружения JVM и OS.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/env")
    public ResponseEntity<Map<String, String>> getEnv() {
        return ResponseEntity.ok(System.getenv());
    }
    @Operation(summary = "Системные свойства", description = "Возвращает текущие системные свойства, переданные в JVM.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/system-properties")
    public ResponseEntity<Map<String, String>> getSystemProperties() {
        Map<String, String> props = new HashMap<>();
        System.getProperties().forEach((k, v) -> props.put(k.toString(), v.toString()));
        return ResponseEntity.ok(props);
    }
    @Operation(summary = "Память JVM", description = "Показывает объём используемой памяти в куче и вне кучи.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/memory-heap")
    public ResponseEntity<Map<String, Object>> getHeapStats() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> map = new HashMap<>();
        map.put("heap.used", memoryMXBean.getHeapMemoryUsage().getUsed());
        map.put("heap.max", memoryMXBean.getHeapMemoryUsage().getMax());
        map.put("nonHeap.used", memoryMXBean.getNonHeapMemoryUsage().getUsed());
        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Проверка состояния системы", description = "Простой health-check, возвращает OK, если все базовые компоненты в порядке.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "OK");
        map.put("timestamp", System.currentTimeMillis());
        map.put("db.connection", false);

        try (Connection conn = dataSource.getConnection()) {
            map.put("db.connection", conn.isValid(2));
        } catch (Exception e) {
            map.put("db.error", e.getMessage());
        }

        map.put("jvm.freeMemory", Runtime.getRuntime().freeMemory());
        map.put("jvm.totalMemory", Runtime.getRuntime().totalMemory());

        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Изменение уровня логов", description = "Позволяет изменить уровень логирования на лету. Пример уровней: DEBUG, INFO, WARN, ERROR.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/log-level")
    public ResponseEntity<String> setLogLevel(@RequestParam String logger, @RequestParam String level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger log = context.getLogger(logger);

        try {
            log.setLevel(Level.valueOf(level.toUpperCase()));
            return ResponseEntity.ok("Уровень логирования для '" + logger + "' изменён на " + level.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Недопустимый уровень логирования: " + level);
        }
    }
    @Operation(summary = "Список временных файлов", description = "Отображает файлы из временной директории. Можно указать путь.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/tmp-files")
    public ResponseEntity<List<String>> listTmpFiles(@RequestParam(defaultValue = "/tmp") String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return ResponseEntity.badRequest().body(List.of("Путь не существует или не является директорией: " + path));
        }

        String[] files = dir.list();
        if (files == null) return ResponseEntity.ok(List.of("Файлы отсутствуют"));

        return ResponseEntity.ok(Arrays.asList(files));
    }
    @Operation(summary = "Информация о диске", description = "Показывает объём занятого и доступного места на основном диске.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/disk-usage")
    public ResponseEntity<Map<String, Object>> getDiskUsage() {
        Map<String, Object> map = new HashMap<>();
        File root = new File("/");

        map.put("disk.total", root.getTotalSpace());
        map.put("disk.free", root.getFreeSpace());
        map.put("disk.usable", root.getUsableSpace());

        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Метрики Garbage Collector", description = "Отображает общее количество и время срабатываний GC.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> getGcStats() {
        Map<String, Object> map = new HashMap<>();

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            Map<String, Object> gcData = new HashMap<>();
            gcData.put("collectionCount", gcBean.getCollectionCount());
            gcData.put("collectionTime", gcBean.getCollectionTime());
            map.put(gcBean.getName(), gcData);
        }

        return ResponseEntity.ok(map);
    }

    @Operation(summary = "Все логи Docker Compose", description = "Возвращает весь вывод команды docker-compose logs.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/docker-logs")
    public ResponseEntity<Map<String, Object>> getDockerLogs() {
        Map<String, Object> result = new HashMap<>();
        StringBuilder logs = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sh", "-c", "docker-compose logs");

            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            result.put("exitCode", exitCode);
            result.put("logs", logs.toString());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }


    @Operation(summary = "Загруженные классы JVM", description = "Показывает общее количество загруженных классов и их размер.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/class-loading")
    public ResponseEntity<Map<String, Object>> getClassLoadingStats() {
        var classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        Map<String, Object> map = new HashMap<>();
        map.put("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        map.put("totalLoadedClassCount", classLoadingMXBean.getTotalLoadedClassCount());
        map.put("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());
        return ResponseEntity.ok(map);
    }
    @Operation(summary = "Загрузка памяти в процентах", description = "Показывает процент использования heap/non-heap памяти.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/memory-percent")
    public ResponseEntity<Map<String, Object>> getMemoryPercentage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> map = new HashMap<>();

        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double heapPercent = (heapUsed * 100.0) / heapMax;

        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
        double nonHeapPercent = nonHeapMax > 0 ? (nonHeapUsed * 100.0) / nonHeapMax : 0;

        map.put("heap.percent", heapPercent);
        map.put("nonHeap.percent", nonHeapPercent);

        return ResponseEntity.ok(map);
    }

}
