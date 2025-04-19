package fr.mossaab.security.controller;

import com.sun.management.OperatingSystemMXBean; // Для расширенных методов CPU/RAM
import fr.mossaab.security.dto.advertisement.AdTimeLeftResponse;
import fr.mossaab.security.dto.advertisement.AdvertisementResponse;
import fr.mossaab.security.dto.user.UserPointsResponse;
import fr.mossaab.security.service.AdvertisementQueueService;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequiredArgsConstructor
public class QuizController {
    private static final String SHORT_RUSSIAN_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1MMVtuIGycNieRu1qvbsstNryl3InC_tseeNWDmyhjLk/export?format=csv";
    private static final String LONG_RUSSIAN_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1M2DU2WwyixNsS0pYZ8-2mULZ4oz_m4L3y6kebmvMexE/export?format=csv";
    private static final String SHORT_ENGLISH_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1m5pBlwX__rKziOGPydrtpaRdF2VvHeQrx9rkMj_wyQM/export?format=csv";
    private static final String LONG_ENGLISH_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1mSfzFeaCPACMIqE3AXQdipaXu5Hvz79zEAHXjBZkrBM/export?format=csv";
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final AdvertisementQueueService advertisementQueueService;
    private final Map<String, List<Question>> cachedQuestionsMap = new HashMap<>();
    private Quiz currentQuiz;

    @PostConstruct
    public void onStartup() {
        reloadQuestionsCacheInternal(); // остальное по желанию
        startNewQuiz(); // ✅ старт первой викторины
    }
    @Operation(summary = "Оставшееся время текущей викторины")
    @GetMapping("/quiz-time-left")
    public ResponseEntity<Map<String, Object>> getQuizTimeLeft() {
        if (currentQuiz == null || !"ACTIVE".equals(currentQuiz.getStatus())) {
            return ResponseEntity.ok(Map.of("message", "Нет активной викторины"));
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsElapsed = java.time.Duration.between(currentQuiz.getStartTime(), now).getSeconds();

        if (secondsElapsed >= currentQuiz.getDuration() * 60) {
            return ResponseEntity.ok(Map.of("minutesLeft", 0, "secondsLeft", 0, "message", "Викторина завершена"));
        }

        long remainingSeconds = currentQuiz.getDuration() * 60 - secondsElapsed;
        int minutesLeft = (int) (remainingSeconds / 60);
        int secondsLeft = (int) (remainingSeconds % 60);

        return ResponseEntity.ok(Map.of(
                "minutesLeft", minutesLeft,
                "secondsLeft", secondsLeft,
                "message", String.format("Осталось %02d:%02d до конца викторины", minutesLeft, secondsLeft)
        ));
    }

    public void startNewQuiz() {
        this.currentQuiz = Quiz.builder()
                .startTime(LocalDateTime.now())
                .duration(60)
                .status("ACTIVE")
                .totalPoints(0)
                .build();

        quizRepository.save(currentQuiz);
        System.out.println("🚀 Новая викторина запущена: " + currentQuiz.getStartTime());
    }


    @PostConstruct
    public void init() {
        reloadQuestionsCacheInternal();
    }
    @Scheduled(fixedRate = 60000) // проверяем каждую минуту
    public void checkAndRotateQuiz() {
        if (currentQuiz == null || !"ACTIVE".equals(currentQuiz.getStatus())) {
            System.out.println("⛔ Нет активной викторины — запуск новой.");
            startNewQuiz();
            return;
        }

        long minutesElapsed = java.time.Duration.between(currentQuiz.getStartTime(), LocalDateTime.now()).toMinutes();
        if (minutesElapsed >= currentQuiz.getDuration()) {
            System.out.println("⏰ Викторина завершена автоматически — запуск новой.");
            endQuiz(currentQuiz);
            startNewQuiz();
        } else {
            System.out.println("⏳ Викторина ещё идёт: прошло " + minutesElapsed + " мин.");
        }
    }
    @Operation(summary = "Оставшееся время текущей рекламы-лидера")
    @GetMapping("/ad-leader-time-left")
    public ResponseEntity<AdTimeLeftResponse> getRemainingAdLeaderTime() {
        return ResponseEntity.ok(advertisementQueueService.getRemainingTimeForCurrentLeader());
    }
    @Operation(summary = "Принудительное обновление лидера рекламы",
            description = "Обновляет лидера рекламы вручную через сервис очереди рекламы. Требуется роль администратора.")
    @PostMapping("/force-next-leader")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> forceNextLeader() {
        advertisementQueueService.updateLeadership();
        return ResponseEntity.ok("Лидер обновлён вручную.");
    }
    @Operation(summary = "Получить текущего лидера рекламы")
    @GetMapping("/current-leader")
    public ResponseEntity<AdvertisementResponse> getCurrentLeader() {
        advertisementQueueService.updateLeadership();
        return advertisementQueueService.getCurrentLeader()
                .map(ad -> ResponseEntity.ok(AdvertisementResponse.builder()
                        .position(1)
                        .cost(ad.getCost())
                        .nickname(ad.getUser().getNickname())
                        .fileDataId(ad.getFileData() != null ? ad.getFileData().getId() : null)
                        .build()))
                .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

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

    @Operation(summary = "Получить случайный вопрос из кэша")
    @GetMapping("/random-question")
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
    @Operation(summary = "Перезагрузка кэша вопросов",
            description = "Очищает и обновляет кэш вопросов, возвращая сообщение об успешной операции.")
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

    @PostConstruct
    public void warmUpCache() {
        // Прогреваем кэш для всех комбинаций вопросов
        getCachedQuestions(QuestionCategory.SHORT, QuestionType.RUSSIAN);
        getCachedQuestions(QuestionCategory.LONG, QuestionType.RUSSIAN);
        getCachedQuestions(QuestionCategory.SHORT, QuestionType.ENGLISH);
        getCachedQuestions(QuestionCategory.LONG, QuestionType.ENGLISH);
        System.out.println("Кэш вопросов успешно прогрет");
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
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionRepository.findAll());
    }

    @Cacheable(value = "questionsCache", key = "#category.name() + '_' + #type.name()")
    public List<Question> getCachedQuestions(QuestionCategory category, QuestionType type) {
        return questionRepository.findByCategoryAndType(category, type);
    }

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

            int adRevenue = advertisementQueueService.calculateAdRevenueForLastHour();

            if (adRevenue > 0) {
                winner.setPears(winner.getPears() + adRevenue);
                System.out.println("Начислено " + adRevenue + " груш за рекламу.");
            } else {
                winner.setPears(winner.getPears() + 10); // фиксированная награда
                System.out.println("Начислено 10 груш по умолчанию (не было активной рекламы).");
            }
        } else {
            System.out.println("Победитель не определен");
        }

        for (User user : users) {
            user.setPoints(0);
        }
        userRepository.saveAll(users);

        // Сброс лидера и очереди рекламы
        advertisementQueueService.resetAdQueue();

        quizRepository.save(quiz);
    }

}
