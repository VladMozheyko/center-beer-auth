package fr.mossaab.security.controller;

import com.sun.management.OperatingSystemMXBean; // Для расширенных методов CPU/RAM
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Question;
import fr.mossaab.security.entities.Quiz;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.enums.QuestionCategory;
import fr.mossaab.security.enums.QuestionType;
import fr.mossaab.security.repository.AdvertisementRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.QuestionRepository;
import fr.mossaab.security.repository.QuizRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Tag(name = "Викторина", description = "API для работы с викториной. Редактирование и удаление элементов осуществляется уже посредством google tables")
@RestController
@RequestMapping("/quiz")
@AllArgsConstructor
public class QuizController {
    private static final String SHORT_RUSSIAN_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1MMVtuIGycNieRu1qvbsstNryl3InC_tseeNWDmyhjLk/export?format=csv";
    private static final String LONG_RUSSIAN_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1M2DU2WwyixNsS0pYZ8-2mULZ4oz_m4L3y6kebmvMexE/export?format=csv";
    private static final String SHORT_ENGLISH_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1m5pBlwX__rKziOGPydrtpaRdF2VvHeQrx9rkMj_wyQM/export?format=csv";
    private static final String LONG_ENGLISH_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1mSfzFeaCPACMIqE3AXQdipaXu5Hvz79zEAHXjBZkrBM/export?format=csv";
    @Autowired
    private DataSource dataSource;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final FileDataRepository fileDataRepository;
    private final AdvertisementRepository advertisementRepository;
    private final StorageService storageService;
    private final Map<String, List<Question>> cachedQuestionsMap = new HashMap<>();

    @PostConstruct
    public void warmUpQuestionsCache() {
        // Загружаем все вопросы из БД
        List<Question> allQuestions = questionRepository.findAll();

        // Группируем по Category + Type
        for (Question question : allQuestions) {
            String key = generateKey(question.getCategory(), question.getType());
            cachedQuestionsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(question);
        }

        System.out.println("✅ Кэш вопросов загружен: " + allQuestions.size() + " вопросов.");
    }

    @Operation(summary = "Получить случайный вопрос из кэша (без БД)")
    @GetMapping("/random-question-fast")
    public ResponseEntity<Question> getRandomQuestionFast(
            @RequestParam QuestionCategory category,
            @RequestParam QuestionType type
    ) {
        String key = generateKey(category, type);

        List<Question> questions = cachedQuestionsMap.getOrDefault(key, Collections.emptyList());
        if (questions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(questions.size());
        Question randomQuestion = questions.get(randomIndex);
        return ResponseEntity.ok(randomQuestion);
    }

    private String generateKey(QuestionCategory category, QuestionType type) {
        return category.name() + "_" + type.name();
    }
    public void reloadQuestionsCacheInternal() {
        cachedQuestionsMap.clear();
        warmUpQuestionsCache();
    }
    // ✅ Метод для обновления кэша вручную
    @PostMapping("/reload-cache")
    public ResponseEntity<String> reloadQuestionsCache() {
        cachedQuestionsMap.clear();
        warmUpQuestionsCache();
        return ResponseEntity.ok("Кэш вопросов перезагружен");
    }
    @Operation(summary = "Список пользователей с ненулевыми очками в порядке убывания")
    @GetMapping("/users-with-points")
    public ResponseEntity<List<UserPointsResponse>> getUsersWithPoints() {
        List<User> usersWithPoints = userRepository.findAll().stream()
                .filter(user -> user.getPoints() > 0)
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
                .collect(Collectors.toList());

        List<UserPointsResponse> response = new ArrayList<>();
        int position = 1;
        for (User user : usersWithPoints) {
            response.add(UserPointsResponse.builder()
                    .position(position)
                    .nickname(user.getNickname())
                    .points(user.getPoints())
                    .build());
            position++;
        }
        return ResponseEntity.ok(response);
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserPointsResponse {
        private int position;
        private String nickname;
        private int points;
    }

    @Operation(summary = "Получение идентификатора fileData рекламы с наибольшей стоимостью")
    @GetMapping("/advertisement-max-cost-file")
    public ResponseEntity<Long> getFileDataIdOfMaxCostAdvertisement() {
        Optional<Advertisement> maxCostAdvertisement = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getFileData() != null)
                .max(Comparator.comparingInt(Advertisement::getCost));

        if (maxCostAdvertisement.isEmpty()) {
            throw new RuntimeException("Не найдено реклам, связанных с файлами.");
        }
        FileData fileData = maxCostAdvertisement.get().getFileData();
        return ResponseEntity.ok(fileData.getId());
    }

    @Operation(summary = "Создание рекламы")
    @PostMapping("/add-advertisements")
    public ResponseEntity<String> createAdvertisement(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam Integer cost) throws IOException {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Найденная почта пользователя: " + userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getPears() < cost) {
            throw new RuntimeException("Недостаточно груш для создания рекламы. Требуется: " + cost + ", доступно: " + user.getPears());
        }
        user.setPears(user.getPears() - cost);
        userRepository.save(user);

        Advertisement advertisement = Advertisement.builder()
                .title(title)
                .description(description)
                .createdAt(LocalDateTime.now())
                .cost(cost)
                .status(AdvertisementStatus.PENDING)
                .user(user)
                .build();

        if (file != null && !file.isEmpty()) {
            FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(file, advertisement);
            fileDataRepository.save(uploadImage);
            advertisement.setFileData(uploadImage);
        }
        advertisement = advertisementRepository.save(advertisement);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Реклама успешно опубликована и добавлена в список.");
    }

    @Operation(summary = "Получение рекламы по убыванию стоимости с выводом идентификатора fileData")
    @GetMapping("/advertisements-by-cost")
    public ResponseEntity<List<AdvertisementResponse>> getAdvertisementsByCost() {
        List<Advertisement> advertisements = advertisementRepository.findAll();
        advertisements.sort((a1, a2) -> Integer.compare(a2.getCost(), a1.getCost()));

        List<AdvertisementResponse> response = new ArrayList<>();
        int position = 1;
        for (Advertisement ad : advertisements) {
            AdvertisementResponse adResponse = AdvertisementResponse.builder()
                    .position(position)
                    .cost(ad.getCost())
                    .nickname(ad.getUser().getNickname())
                    .fileDataId(ad.getFileData() != null ? ad.getFileData().getId() : null)
                    .build();
            response.add(adResponse);
            position++;
        }
        return ResponseEntity.ok(response);
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdvertisementResponse {
        private int position;
        private int cost;
        private String nickname;
        private Long fileDataId;
    }
    @PostConstruct
    public void warmUpCache() {
        // Прогреваем кэш для всех комбинаций вопросов
        getCachedQuestions(QuestionCategory.SHORT, QuestionType.RUSSIAN);
        getCachedQuestions(QuestionCategory.LONG, QuestionType.RUSSIAN);
        getCachedQuestions(QuestionCategory.SHORT, QuestionType.ENGLISH);
        getCachedQuestions(QuestionCategory.LONG, QuestionType.ENGLISH);
        System.out.println("Кэш вопросов успешно прогрет");
    }
    private String getCsvLinkByCategoryAndType(QuestionCategory category, QuestionType type) {
        if (category == QuestionCategory.SHORT && type == QuestionType.RUSSIAN) {
            return SHORT_RUSSIAN_QUESTIONS_URL;
        } else if (category == QuestionCategory.LONG && type == QuestionType.RUSSIAN) {
            return LONG_RUSSIAN_QUESTIONS_URL;
        } else if (category == QuestionCategory.SHORT && type == QuestionType.ENGLISH) {
            return SHORT_ENGLISH_QUESTIONS_URL;
        } else if (category == QuestionCategory.LONG && type == QuestionType.ENGLISH) {
            return LONG_ENGLISH_QUESTIONS_URL;
        } else {
            throw new RuntimeException("Неизвестная комбинация category=" + category + " и type=" + type);
        }
    }

    @Operation(summary = "Обновление вопросов (подгружает все вопросы)")
    @PostMapping("/update-from-csv")
    @CacheEvict(value = "questionsCache", allEntries = true)
    public String updateQuestionsFromCSV() {
        try {
            // Очистка таблицы вопросов
            questionRepository.deleteAll();

            List<Question> all = new ArrayList<>();
            all.addAll(parseQuestionsFromUrl(SHORT_RUSSIAN_QUESTIONS_URL, QuestionCategory.SHORT, QuestionType.RUSSIAN));
            all.addAll(parseQuestionsFromUrl(LONG_RUSSIAN_QUESTIONS_URL, QuestionCategory.LONG, QuestionType.RUSSIAN));
            all.addAll(parseQuestionsFromUrl(SHORT_ENGLISH_QUESTIONS_URL, QuestionCategory.SHORT, QuestionType.ENGLISH));
            all.addAll(parseQuestionsFromUrl(LONG_ENGLISH_QUESTIONS_URL, QuestionCategory.LONG, QuestionType.ENGLISH));

            questionRepository.saveAll(all);
            return "Обновлены ВСЕ вопросы (4 комбинации). Добавлено: " + all.size();
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при обновлении вопросов: " + e.getMessage();
        }
    }

    private List<Question> parseQuestionsFromUrl(String csvUrl, QuestionCategory category, QuestionType type)
            throws IOException, CsvException {
        List<Question> resultList = new ArrayList<>();
        try (InputStream inputStream = new URL(csvUrl).openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 7) {
                    System.out.println("Пропущена строка, т.к. меньше 7 столбцов: " + Arrays.toString(row));
                    continue;
                }
                Question question = Question.builder()
                        .text(row[1])
                        .optionA(row[2])
                        .optionB(row[3])
                        .optionC(row[4])
                        .optionD(row[5])
                        .correctAnswer(row[6])
                        .category(category)
                        .type(type)
                        .build();
                resultList.add(question);
            }
        }
        return resultList;
    }

    @Operation(summary = "Вывод всех вопросов")
    @GetMapping("/get-all-questions")
    public ResponseEntity<List<Question>> getAllQuestions(HttpServletRequest request) {
        return ResponseEntity.ok(questionRepository.findAll());
    }

    @Cacheable(value = "questionsCache", key = "#category.name() + '_' + #type.name()")
    public List<Question> getCachedQuestions(QuestionCategory category, QuestionType type) {
        return questionRepository.findByCategoryAndType(category, type);
    }
    @Async
    public CompletableFuture<Question> getRandomQuestionAsync(QuestionCategory category, QuestionType type) {
        List<Question> questions = getCachedQuestions(category, type);
        if (questions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(questions.size());
        return CompletableFuture.completedFuture(questions.get(randomIndex));
    }

//    @GetMapping("/random-question")
//    public CompletableFuture<ResponseEntity<Question>> getRandomQuestion(
//            @RequestParam QuestionCategory category,
//            @RequestParam QuestionType type
//    ) {
//        return getRandomQuestionAsync(category, type)
//                .thenApply(question -> {
//                    if (question == null) {
//                        return ResponseEntity.noContent().build();
//                    }
//                    return ResponseEntity.ok(question);
//                });
//    }

    @Operation(summary = "Ответить на вопрос (короткий или длинный)")
    @PostMapping("/submit-answer")
    public ResponseEntity<Integer> submitAnswer(
            @RequestParam QuestionCategory category,
            @RequestParam Long questionId,
            @RequestParam String userAnswer) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        if (!category.equals(question.getCategory())) {
            throw new RuntimeException("Вопрос не относится к указанной категории: " + category);
        }

        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim());
        if (QuestionCategory.SHORT.equals(category)) {
            if (isCorrect) {
                user.setPoints(user.getPoints() + 10);
            } else {
                if (user.getPoints() >= 5) {
                    user.setPoints(user.getPoints() - 5);
                }
            }
        } else if (QuestionCategory.LONG.equals(category)) {
            if (isCorrect) {
                user.setPoints(user.getPoints() + 20);
            } else {
                if (user.getPoints() >= 10) {
                    user.setPoints(user.getPoints() - 10);
                }
            }
        }
        userRepository.save(user);
        return ResponseEntity.ok(user.getPoints());
    }

    @Scheduled(fixedRate = 3600000)
    public void startQuizAutomatically() {
        Quiz quiz = Quiz.builder()
                .startTime(LocalDateTime.now())
                .duration(60)
                .status("ACTIVE")
                .totalPoints(0)
                .build();
        quiz = quizRepository.save(quiz);
        System.out.println("Новая викторина запущена с ID: " + quiz.getId());
        Quiz finalQuiz = quiz;
        Executors.newSingleThreadScheduledExecutor().schedule(() -> endQuiz(finalQuiz), finalQuiz.getDuration(), TimeUnit.MINUTES);
    }

    @Operation(summary = "Получение текущих очков пользователя")
    @GetMapping("/current-user/points")
    public ResponseEntity<Integer> getCurrentUserPoints() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return ResponseEntity.ok(user.getPoints());
    }

    public void endQuiz(Quiz quiz) {
        Optional<Quiz> optionalQuiz = quizRepository.findById(quiz.getId());
        if (optionalQuiz.isEmpty() || !"ACTIVE".equals(optionalQuiz.get().getStatus())) {
            System.out.println("Викторина уже завершена или не найдена");
            return;
        }

        quiz.setStatus("COMPLETED");

        List<User> users = userRepository.findAll();
        User winner = null;
        int maxPoints = 0;
        for (User user : users) {
            if (user.getPoints() > maxPoints) {
                maxPoints = user.getPoints();
                winner = user;
            }
        }
        if (winner != null) {
            System.out.println("Победитель викторины: " + winner.getNickname() + " с " + maxPoints + " очками");
        } else {
            System.out.println("Победитель не определен");
        }
        for (User user : users) {
            user.setPoints(0);
        }
        userRepository.saveAll(users);
        advertisementRepository.deleteAll();
        quizRepository.save(quiz);
    }

    @Operation(summary = "Получение оставшегося времени текущей викторины")
    @GetMapping("/remaining-time")
    public ResponseEntity<String> getRemainingTime() {
        String remainingTime;
        List<Quiz> quizzes = quizRepository.findAll();

        Quiz activeQuiz = null;
        for (Quiz quiz : quizzes) {
            if ("ACTIVE".equalsIgnoreCase(quiz.getStatus())) {
                if (activeQuiz == null || quiz.getStartTime().isAfter(activeQuiz.getStartTime())) {
                    activeQuiz = quiz;
                }
            }
        }
        if (activeQuiz == null) {
            throw new RuntimeException("Нет активной викторины");
        }
        LocalDateTime endTime = activeQuiz.getStartTime().plusMinutes(activeQuiz.getDuration());
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            remainingTime = "Викторина завершена.";
        } else {
            long totalSeconds = java.time.Duration.between(now, endTime).getSeconds();
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            remainingTime = String.format("%02d:%02d", minutes, seconds);
        }
        return ResponseEntity.ok(remainingTime);
    }

    @GetMapping("/monitor/system-stats")
    public Map<String, Object> getSystemStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            // ------------------------------
            // 1) Общая информация о JVM/ОС
            // ------------------------------
            result.put("os.name", System.getProperty("os.name"));
            result.put("os.arch", System.getProperty("os.arch"));
            result.put("os.version", System.getProperty("os.version"));

            // JVM Memory
            Runtime runtime = Runtime.getRuntime();
            result.put("jvm.availableProcessors", runtime.availableProcessors());
            result.put("jvm.totalMemory", runtime.totalMemory());
            result.put("jvm.freeMemory", runtime.freeMemory());
            result.put("jvm.maxMemory", runtime.maxMemory());

            // ------------------------------
            // 2) Использование CPU/Memory на уровне ОС
            // ------------------------------
            // OperatingSystemMXBean (com.sun.management.*) даёт расширенные методы
            java.lang.management.OperatingSystemMXBean baseOsBean =
                    ManagementFactory.getOperatingSystemMXBean();

            if (baseOsBean instanceof OperatingSystemMXBean) {
                OperatingSystemMXBean osBean = (OperatingSystemMXBean) baseOsBean;
                // Загрузка CPU процессом (0.0 ... 1.0)
                result.put("processCpuLoad", osBean.getProcessCpuLoad());
                // Загрузка всей системы (0.0 ... 1.0)
                result.put("systemCpuLoad", osBean.getSystemCpuLoad());

                // Физическая память (байты)
                long freePhysMem = osBean.getFreePhysicalMemorySize();
                long totalPhysMem = osBean.getTotalPhysicalMemorySize();
                result.put("os.freePhysicalMemorySize", freePhysMem);
                result.put("os.totalPhysicalMemorySize", totalPhysMem);
            }

            // ------------------------------
            // 3) Статистика Tomcat (через MBeans)
            // ------------------------------
            // Если используется встроенный Tomcat (Spring Boot), название ObjectName может отличаться
            // Обычно что-то вроде "Tomcat:type=ThreadPool,name=\"http-nio-8080\""
            try {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName threadPoolMBeanName = new ObjectName("Tomcat:type=ThreadPool,name=\"http-nio-8080\"");

                Integer currentThreadsBusy = (Integer) mBeanServer.getAttribute(threadPoolMBeanName, "currentThreadsBusy");
                Integer currentThreadCount = (Integer) mBeanServer.getAttribute(threadPoolMBeanName, "currentThreadCount");

                result.put("tomcat.currentThreadsBusy", currentThreadsBusy);
                result.put("tomcat.currentThreadCount", currentThreadCount);
            } catch (Exception e) {
                // Если не найдёт MBean (порт другой или не Tomcat)
                result.put("tomcat.error", e.getMessage());
            }

            // ------------------------------
            // 4) Статистика MySQL (SHOW GLOBAL STATUS / VARIABLES)
            // ------------------------------
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 4a) SHOW GLOBAL STATUS
                try (ResultSet rs = stmt.executeQuery("SHOW GLOBAL STATUS")) {
                    Map<String, String> globalStatus = new HashMap<>();
                    while (rs.next()) {
                        String variableName = rs.getString(1);
                        String value = rs.getString(2);
                        globalStatus.put(variableName, value);
                    }
                    result.put("mysql.globalStatus", globalStatus);
                }

                // 4b) SHOW GLOBAL VARIABLES
                try (ResultSet rs = stmt.executeQuery("SHOW GLOBAL VARIABLES")) {
                    Map<String, String> globalVariables = new HashMap<>();
                    while (rs.next()) {
                        String variableName = rs.getString(1);
                        String value = rs.getString(2);
                        globalVariables.put(variableName, value);
                    }
                    result.put("mysql.globalVariables", globalVariables);
                }
            }

        } catch (Exception ex) {
            result.put("error", ex.getMessage());
        }

        // Результат будет автоматически возвращён в JSON (при наличии Jackson)
        return result;
    }
}
