package fr.mossaab.security.controller;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.enums.QuestionCategory;
import fr.mossaab.security.enums.QuestionType;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Tag(name = "Викторина", description = "API для работы с викториной. Редактирование и удаление элементов осуществляется уже посредством google tables")

@RestController
@RequestMapping("/quiz")
@AllArgsConstructor
public class QuizController {
    //    private static final String SHORT_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1MMVtuIGycNieRu1qvbsstNryl3InC_tseeNWDmyhjLk/export?format=csv";
//    private static final String LONG_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1M2DU2WwyixNsS0pYZ8-2mULZ4oz_m4L3y6kebmvMexE/export?format=csv";
//
// Русские
    private static final String SHORT_RUSSIAN_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1MMVtuIGycNieRu1qvbsstNryl3InC_tseeNWDmyhjLk/export?format=csv";
    private static final String LONG_RUSSIAN_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1M2DU2WwyixNsS0pYZ8-2mULZ4oz_m4L3y6kebmvMexE/export?format=csv";
    // Английские
    private static final String SHORT_ENGLISH_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1m5pBlwX__rKziOGPydrtpaRdF2VvHeQrx9rkMj_wyQM/export?format=csv";
    private static final String LONG_ENGLISH_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1mSfzFeaCPACMIqE3AXQdipaXu5Hvz79zEAHXjBZkrBM/export?format=csv";
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final FileDataRepository fileDataRepository;
    private final AdvertisementRepository advertisementRepository;
    private final StorageService storageService;

    @Operation(summary = "Список пользователей с ненулевыми очками в порядке убывания")
    @GetMapping("/users-with-points")
    public ResponseEntity<List<UserPointsResponse>> getUsersWithPoints() {
        // Получаем всех пользователей с ненулевыми очками
        List<User> usersWithPoints = userRepository.findAll().stream()
                .filter(user -> user.getPoints() > 0) // Фильтруем пользователей с ненулевыми очками
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints())) // Сортируем по убыванию очков
                .toList();

        // Формируем список ответов с позициями
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

    // DTO для ответа
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserPointsResponse {
        private int position; // Позиция в списке
        private String nickname; // Никнейм пользователя
        private int points; // Очки пользователя
    }

    @Operation(summary = "Получение идентификатора fileData рекламы с наибольшей стоимостью")
    @GetMapping("/advertisement-max-cost-file")
    public ResponseEntity<Long> getFileDataIdOfMaxCostAdvertisement() {
        // Находим рекламу с наибольшей стоимостью
        Optional<Advertisement> maxCostAdvertisement = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getFileData() != null) // Убедимся, что у рекламы есть связанный FileData
                .max(Comparator.comparingInt(Advertisement::getCost)); // Сравниваем по стоимости

        // Если реклама с максимальной стоимостью не найдена
        if (maxCostAdvertisement.isEmpty()) {
            throw new RuntimeException("Не найдено реклам, связанных с файлами.");
        }

        // Получаем связанный fileData и его идентификатор
        FileData fileData = maxCostAdvertisement.get().getFileData();

        return ResponseEntity.ok(fileData.getId());
    }


    @Operation(summary = "Создание рекламы")
    @PostMapping("/add-advertisements")
    public ResponseEntity createAdvertisement(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile file, // Необязательный файл для рекламы
            @RequestParam Integer cost) throws IOException {
        // Получаем текущего пользователя
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Найденная почта пользователя: " + userEmail); // Вывод в консоль
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем, достаточно ли груш у пользователя
        if (user.getPears() < cost) {
            throw new RuntimeException("Недостаточно груш для создания рекламы. Требуется: " + cost + ", доступно: " + user.getPears());
        }

        // Вычитаем количество груш
        user.setPears(user.getPears() - cost);
        userRepository.save(user);

        // Создаём объект Advertisement
        Advertisement advertisement = Advertisement.builder()
                .title(title)
                .description(description)
                .createdAt(LocalDateTime.now())
                .cost(cost)
                .status(AdvertisementStatus.PENDING)
                .user(user)
                .build();

        // Если передан файл, обрабатываем его и связываем с рекламой
        if (file != null && !file.isEmpty()) {
            FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(file, advertisement);
            fileDataRepository.save(uploadImage);
            advertisement.setFileData(uploadImage);
        }
        // Сохраняем рекламу
        advertisement = advertisementRepository.save(advertisement);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Реклама успешно опубликована и добавлена в список.");
    }


    @Operation(summary = "Получение рекламы по убыванию стоимости с выводом идентификатора fileData")
    @GetMapping("/advertisements-by-cost")
    public ResponseEntity<List<AdvertisementResponse>> getAdvertisementsByCost() {
        // Получаем все рекламные объявления
        List<Advertisement> advertisements = advertisementRepository.findAll();

        // Сортируем объявления по убыванию стоимости (cost)
        advertisements.sort((a1, a2) -> Integer.compare(a2.getCost(), a1.getCost()));

        // Формируем список ответов
        List<AdvertisementResponse> response = new ArrayList<>();
        int position = 1;
        for (Advertisement ad : advertisements) {
            AdvertisementResponse adResponse = AdvertisementResponse.builder()
                    .position(position)
                    .cost(ad.getCost())
                    .nickname(ad.getUser().getNickname())
                    .fileDataId(ad.getFileData() != null ? ad.getFileData().getId() : null) // Добавляем идентификатор fileData
                    .build();
            response.add(adResponse);
            position++;
        }

        return ResponseEntity.ok(response);
    }


    // DTO для ответа
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdvertisementResponse {
        private int position; // Позиция в списке
        private int cost; // Стоимость
        private String nickname; // Никнейм пользователя
        private Long fileDataId; // Идентификатор fileData
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
    @Operation(summary = "Обновление вопросов (короткий/длинный, русский/английский)")
    @PostMapping("/update-from-csv")
    public String updateQuestionsFromCSV(
            @RequestParam(required = false) QuestionCategory category,
            @RequestParam(required = false) QuestionType type
    ) {
        try {
            // Очистить таблицу вопросов (при необходимости)
            questionRepository.deleteAll();

            // 1) Если НЕ переданы ни category, ни type => загружаем все 4 варианта
            if (category == null && type == null) {
                List<Question> all = new ArrayList<>();
                all.addAll(parseQuestionsFromUrl(SHORT_RUSSIAN_QUESTIONS_URL, QuestionCategory.SHORT, QuestionType.RUSSIAN));
                all.addAll(parseQuestionsFromUrl(LONG_RUSSIAN_QUESTIONS_URL,  QuestionCategory.LONG,  QuestionType.RUSSIAN));
                all.addAll(parseQuestionsFromUrl(SHORT_ENGLISH_QUESTIONS_URL, QuestionCategory.SHORT, QuestionType.ENGLISH));
                all.addAll(parseQuestionsFromUrl(LONG_ENGLISH_QUESTIONS_URL,  QuestionCategory.LONG,  QuestionType.ENGLISH));

                questionRepository.saveAll(all);
                return "Обновлены ВСЕ вопросы (4 комбинации). Добавлено: " + all.size();
            }

            // 2) Если category == null, но type != null => грузим 2 комбинации (SHORT+type, LONG+type)
            if (category == null && type != null) {
                List<Question> all = new ArrayList<>();
                all.addAll(parseQuestionsFromUrl(getCsvLinkByCategoryAndType(QuestionCategory.SHORT, type),
                        QuestionCategory.SHORT, type));
                all.addAll(parseQuestionsFromUrl(getCsvLinkByCategoryAndType(QuestionCategory.LONG,  type),
                        QuestionCategory.LONG,  type));
                questionRepository.saveAll(all);
                return "Обновлены вопросы для type=" + type + " (SHORT и LONG). Добавлено: " + all.size();
            }

            // 3) Если type == null, но category != null => грузим 2 комбинации (category+RUSSIAN, category+ENGLISH)
            if (type == null && category != null) {
                List<Question> all = new ArrayList<>();
                all.addAll(parseQuestionsFromUrl(getCsvLinkByCategoryAndType(category, QuestionType.RUSSIAN),
                        category, QuestionType.RUSSIAN));
                all.addAll(parseQuestionsFromUrl(getCsvLinkByCategoryAndType(category, QuestionType.ENGLISH),
                        category, QuestionType.ENGLISH));
                questionRepository.saveAll(all);
                return "Обновлены вопросы для category=" + category + " (RUSSIAN и ENGLISH). Добавлено: " + all.size();
            }

            // 4) Иначе, если указаны и category, и type => одна комбинация
            List<Question> singleList = parseQuestionsFromUrl(
                    getCsvLinkByCategoryAndType(category, type),
                    category,
                    type
            );
            questionRepository.saveAll(singleList);
            return "Обновлены вопросы для category=" + category + " и type=" + type + ". Добавлено: " + singleList.size();
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при обновлении вопросов: " + e.getMessage();
        }
    }


    /**
     * Вспомогательный метод для парсинга вопросов из CSV по указанному URL и заданной категории.
     */
    private List<Question> parseQuestionsFromUrl(String csvUrl,
                                                 QuestionCategory category,
                                                 QuestionType type)
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
                        .type(type) // <-- Указываем тип
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

    @Operation(summary = "Получение случайного вопроса (короткий/длинный, русский/английский)")
    @GetMapping("/random-question")
    public ResponseEntity<Question> getRandomQuestion(
            @RequestParam QuestionCategory category,
            @RequestParam QuestionType type
    ) {
        // Фильтруем сразу по двум параметрам
        List<Question> questions = questionRepository.findAll().stream()
                .filter(q -> q.getCategory().equals(category))
                .filter(q -> q.getType().equals(type))
                .toList();

        if (questions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Random random = new Random();
        Question randomQuestion = questions.get(random.nextInt(questions.size()));
        return ResponseEntity.ok(randomQuestion);
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

        // Ищем вопрос
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        // Проверяем, что категория совпадает
        if (!category.equals(question.getCategory())) {
            throw new RuntimeException("Вопрос не относится к указанной категории: " + category);
        }

        // Логика начисления очков - пример (можно заменить на свою)
        // Для SHORT: +10 за правильный, -5 за неверный
        // Для LONG: +20 за правильный, -10 за неверный
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


    @Scheduled(fixedRate = 3600000) // Запуск каждые 60 минут (3600000 мс)
    public void startQuizAutomatically() {
        // Создаем новую викторину
        Quiz quiz = Quiz.builder()
                .startTime(LocalDateTime.now())
                .duration(60)
                .status("ACTIVE")
                .totalPoints(0)
                .build();
        quiz = quizRepository.save(quiz);

        System.out.println("Новая викторина запущена с ID: " + quiz.getId());

        // Используем final переменную
        Quiz finalQuiz = quiz;
        Executors.newSingleThreadScheduledExecutor().schedule(() -> endQuiz(finalQuiz), finalQuiz.getDuration(), TimeUnit.MINUTES);
    }

    /**
     * Получение текущих очков пользователя.
     */
    @Operation(summary = "Получение текущих очков пользователя")
    @GetMapping("/current-user/points")
    public ResponseEntity<Integer> getCurrentUserPoints() {
        Integer points;
        // Получаем текущего пользователя
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Получаем количество очков
        points = user.getPoints();

        return ResponseEntity.ok(points);
    }

    /**
     * Завершает викторину, начисляет очки и определяет победителя.
     */
    public void endQuiz(Quiz quiz) {
        Optional<Quiz> optionalQuiz = quizRepository.findById(quiz.getId());
        if (optionalQuiz.isEmpty() || !"ACTIVE".equals(optionalQuiz.get().getStatus())) {
            System.out.println("Викторина уже завершена или не найдена");
            return;
        }

        quiz.setStatus("COMPLETED");

        // Логика определения победителя
        List<User> users = userRepository.findAll(); // Получаем всех пользователей
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

        // Рассчитываем оставшееся время
        if (now.isAfter(endTime)) {
            remainingTime = "Викторина завершена.";
        } else {
            long totalSeconds = java.time.Duration.between(now, endTime).getSeconds();
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;

            // Форматируем время в MM:SS
            remainingTime = String.format("%02d:%02d", minutes, seconds);
        }

        return ResponseEntity.ok(remainingTime);
    }


}
