package fr.mossaab.security.controller;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.enums.QuestionCategory;
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

    //private final AdvertisementRepository advertisementRepository;
    //private static final String EXCEL_URL = "https://docs.google.com/spreadsheets/d/1RU9Nl4ogjWftcVX76wlWgm0gCmFs9Z5xyUSCU3Uz6cc/export?format=csv";
    private static final String SHORT_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1MMVtuIGycNieRu1qvbsstNryl3InC_tseeNWDmyhjLk/export?format=csv";
    private static final String LONG_QUESTIONS_URL = "https://docs.google.com/spreadsheets/d/1M2DU2WwyixNsS0pYZ8-2mULZ4oz_m4L3y6kebmvMexE/export?format=csv";
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


    //    @Operation(summary = "Обновление вопросов в соответствие google table")
//    @PostMapping("/update-from-csv")
//    public String updateQuestionsFromCSV() {
//        InputStream inputStream = null;
//        try {
//            // Очистить таблицу вопросов
//            questionRepository.deleteAll();
//
//            // Загрузить CSV-файл
//            URL url = new URL(EXCEL_URL);
//            inputStream = url.openStream(); // Открываем поток
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            CSVReader csvReader = new CSVReader(reader);
//            List<String[]> rows = csvReader.readAll();
//
//            List<Question> questions = new ArrayList<>();
//
//            // Прочитать строки CSV-файла
//            for (int i = 1; i < rows.size(); i++) { // Пропускаем заголовок
//                String[] row = rows.get(i);
//                Question question = Question.builder()
//                        .text(row[1]) // Вопрос
//                        .optionA(row[2]) // Вариант A
//                        .optionB(row[3]) // Вариант B
//                        .optionC(row[4]) // Вариант C
//                        .optionD(row[5]) // Вариант D
//                        .correctAnswer(row[6]) // Правильный ответ
//                        .build();
//                questions.add(question);
//            }
//
//            // Сохранить вопросы в базу данных
//            questionRepository.saveAll(questions);
//
//            return "Вопросы успешно обновлены: " + questions.size() + " записей добавлено.";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Ошибка при обновлении вопросов: " + e.getMessage();
//        } finally {
//            try {
//                if (inputStream != null) {
//                    inputStream.close();
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//    }
    @Operation(summary = "Обновление вопросов (коротких и длинных) из Google Tables")
    @PostMapping("/update-from-csv")
    public String updateQuestionsFromCSV() {
        try {
            // Очистить таблицу вопросов
            questionRepository.deleteAll();

            // Парсим короткие вопросы
            List<Question> shortQuestions = parseQuestionsFromUrl(SHORT_QUESTIONS_URL, QuestionCategory.SHORT);
            // Парсим длинные вопросы
            List<Question> longQuestions = parseQuestionsFromUrl(LONG_QUESTIONS_URL, QuestionCategory.LONG);

            // Объединяем все вопросы
            List<Question> allQuestions = new ArrayList<>();
            allQuestions.addAll(shortQuestions);
            allQuestions.addAll(longQuestions);

            // Сохраняем в БД
            questionRepository.saveAll(allQuestions);

            return "Вопросы успешно обновлены. Добавлено: " + allQuestions.size() + " вопросов.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при обновлении вопросов: " + e.getMessage();
        }
    }

    /**
     * Вспомогательный метод для парсинга вопросов из CSV по указанному URL и заданной категории.
     */
    private List<Question> parseQuestionsFromUrl(String csvUrl, QuestionCategory category) throws IOException, CsvException {
        List<Question> resultList = new ArrayList<>();

        try (InputStream inputStream = new URL(csvUrl).openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();
            // Предположим, что первая строка — заголовки, поэтому начинаем с i = 1
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                // Будьте внимательны к индексам row[..],
                // они должны совпадать со структурой вашей таблицы CSV.
                System.out.println("Обработка строки №" + i + ": " + Arrays.toString(row));

                if (row.length < 7) {
                    System.out.println("Пропущена строка, так как в ней меньше 7 столбцов");
                    continue;
                }
                Question question = Question.builder()
                        .text(row[1])        // Текст вопроса
                        .optionA(row[2])     // Вариант A
                        .optionB(row[3])     // Вариант B
                        .optionC(row[4])     // Вариант C
                        .optionD(row[5])     // Вариант D
                        .correctAnswer(row[6]) // Правильный ответ
                        .category(category)    // Указываем категорию
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

    //    @Operation(summary = "Получение случайного вопроса")
//    @GetMapping("/random-questions")
//    public ResponseEntity<Question> getRandomQuestion(HttpServletRequest request) {
//        Question randomQuestion = null;
//        List<Question> questions = questionRepository.findAll();
//        if (!questions.isEmpty()) {
//            // Выбираем случайный вопрос
//            Random random = new Random();
//            randomQuestion = questions.get(random.nextInt(questions.size()));
//        }
//        return ResponseEntity.ok(randomQuestion);
//    }
    @Operation(summary = "Получение случайного короткого вопроса")
    @GetMapping("/random-short-question")
    public ResponseEntity<Question> getRandomShortQuestion() {
        // Выбираем все вопросы категории SHORT
        List<Question> shortQuestions = questionRepository.findAll().stream()
                .filter(q -> QuestionCategory.SHORT.equals(q.getCategory()))
                .toList();

        // Если коротких вопросов нет, вернём 204 (No Content) или выбросим исключение
        if (shortQuestions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Случайным образом выбираем один вопрос
        Random random = new Random();
        Question randomShortQuestion = shortQuestions.get(random.nextInt(shortQuestions.size()));

        return ResponseEntity.ok(randomShortQuestion);
    }

    @Operation(summary = "Получение случайного длинного вопроса")
    @GetMapping("/random-long-question")
    public ResponseEntity<Question> getRandomLongQuestion() {
        // Выбираем все вопросы категории LONG
        List<Question> longQuestions = questionRepository.findAll().stream()
                .filter(q -> QuestionCategory.LONG.equals(q.getCategory()))
                .toList();

        if (longQuestions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        Random random = new Random();
        Question randomLongQuestion = longQuestions.get(random.nextInt(longQuestions.size()));

        return ResponseEntity.ok(randomLongQuestion);
    }


//    @Operation(summary = "Ответить на вопрос")
//    @PostMapping("/submit-answer")
//    public ResponseEntity<Integer> submitAnswer(
//            @RequestParam Long questionId,
//            @RequestParam String userAnswer) {
//        Integer updatedPoints = null;
//
//        // Получаем текущего пользователя
//        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        // Получаем пользователя из базы данных
//        User user = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//
//        // Ищем вопрос по ID
//        Question question = questionRepository.findById(questionId)
//                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));
//
//        // Проверяем правильность ответа
//        if (question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim())) {
//            // Увеличиваем очки пользователя
//            user.setPoints(user.getPoints() + 10); // Например, 10 очков за правильный ответ
//        } else {
//            // Вычитаем 10 очков за неправильный ответ, если очки больше 10
//            if (user.getPoints() >= 10) {
//                user.setPoints(user.getPoints() - 5);
//            }
//            // Если очков меньше 10, ничего не делаем
//        }
//
//        // Сохраняем обновлённые данные пользователя
//        userRepository.save(user);
//        updatedPoints = user.getPoints();
//
//        return ResponseEntity.ok(updatedPoints);
//    }

    @Operation(summary = "Ответить на короткий вопрос")
    @PostMapping("/submit-short-answer")
    public ResponseEntity<Integer> submitShortAnswer(
            @RequestParam Long questionId,
            @RequestParam String userAnswer) {

        // Получаем текущего пользователя
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Ищем вопрос по ID
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        // Проверяем, что это короткий вопрос
        if (!QuestionCategory.SHORT.equals(question.getCategory())) {
            throw new RuntimeException("Данный вопрос не относится к категории коротких.");
        }

        // Проверяем правильность ответа
        if (question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim())) {
            // Например, +5 очков за правильный ответ на короткий вопрос
            user.setPoints(user.getPoints() + 5);
        } else {
            // Например, -2 очка за неправильный ответ (не уходим в минус сильнее, чем пользователь имеет)
            if (user.getPoints() >= 2) {
                user.setPoints(user.getPoints() - 2);
            }
        }

        // Сохраняем обновлённые данные пользователя
        userRepository.save(user);
        return ResponseEntity.ok(user.getPoints());
    }

    @Operation(summary = "Ответить на длинный вопрос")
    @PostMapping("/submit-long-answer")
    public ResponseEntity<Integer> submitLongAnswer(
            @RequestParam Long questionId,
            @RequestParam String userAnswer) {

        // Получаем текущего пользователя
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Ищем вопрос по ID
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

        // Проверяем, что это длинный вопрос
        if (!QuestionCategory.LONG.equals(question.getCategory())) {
            throw new RuntimeException("Данный вопрос не относится к категории длинных.");
        }

        // Проверяем правильность ответа
        if (question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim())) {
            // Например, +10 очков за правильный ответ на длинный вопрос
            user.setPoints(user.getPoints() + 10);
        } else {
            // Например, -5 очков за неправильный ответ
            if (user.getPoints() >= 5) {
                user.setPoints(user.getPoints() - 5);
            }
        }

        // Сохраняем обновлённые данные пользователя
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
