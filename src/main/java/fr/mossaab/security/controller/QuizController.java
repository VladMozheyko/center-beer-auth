package fr.mossaab.security.controller;

import fr.mossaab.security.entities.ApiResponse;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.QuestionRepository;
import fr.mossaab.security.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import fr.mossaab.security.entities.Question;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

@Tag(name = "Викторина", description = "API для работы с викториной. Редактирование и удаление элементов осуществляется уже посредством google tables")

@RestController
@RequestMapping("/quiz")
@AllArgsConstructor
public class QuizController {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private static final String EXCEL_URL = "https://docs.google.com/spreadsheets/d/1RU9Nl4ogjWftcVX76wlWgm0gCmFs9Z5xyUSCU3Uz6cc/export?format=csv";
    @Operation(summary = "Обновление вопросов в соответствие google table")
    @PostMapping("/update-from-csv")
    public String updateQuestionsFromCSV() {
        InputStream inputStream = null;
        try {
            // Очистить таблицу вопросов
            questionRepository.deleteAll();

            // Загрузить CSV-файл
            URL url = new URL(EXCEL_URL);
            inputStream = url.openStream(); // Открываем поток
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            CSVReader csvReader = new CSVReader(reader);
            List<String[]> rows = csvReader.readAll();

            List<Question> questions = new ArrayList<>();

            // Прочитать строки CSV-файла
            for (int i = 1; i < rows.size(); i++) { // Пропускаем заголовок
                String[] row = rows.get(i);
                Question question = Question.builder()
                        .text(row[1]) // Вопрос
                        .optionA(row[2]) // Вариант A
                        .optionB(row[3]) // Вариант B
                        .optionC(row[4]) // Вариант C
                        .optionD(row[5]) // Вариант D
                        .correctAnswer(row[6]) // Правильный ответ
                        .build();
                questions.add(question);
            }

            // Сохранить вопросы в базу данных
            questionRepository.saveAll(questions);

            return "Вопросы успешно обновлены: " + questions.size() + " записей добавлено.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при обновлении вопросов: " + e.getMessage();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    @Operation(summary = "Вывод всех вопросов")
    @GetMapping("/get-all-questions")
    public ResponseEntity<ApiResponse<List<Question>>> getAllQuestions(HttpServletRequest request) {
        long startTime = System.nanoTime(); // Засекаем время начала обработки
        List<String> errors = new ArrayList<>();
        List<Question> questions = null;

        try {
            questions = questionRepository.findAll();
        } catch (Exception ex) {
            errors.add("Ошибка при получении вопросов: " + ex.getMessage());
        }

        String user = SecurityContextHolder.getContext().getAuthentication().getName();

        // Формируем ответ
        ApiResponse<List<Question>> response = ApiResponse.<List<Question>>builder()
                .timestamp(LocalDateTime.now())
                .status(errors.isEmpty() ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(errors.isEmpty() ? "Вопросы, варианты ответов и ответ, успешно получены" : "Ошибки при получении данных")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .processingTime(calculateProcessingTime(startTime))
                .user(user)
                .data(questions)
                .count(questions == null ? 0 : questions.size())
                .errors(errors.isEmpty() ? null : errors) // Добавляем список ошибок, если они есть
                .build();

        return ResponseEntity.status(errors.isEmpty() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    @Operation(summary = "Получение случайного вопроса")
    @GetMapping("/random-questions")
    public ResponseEntity<ApiResponse<Question>> getRandomQuestion(HttpServletRequest request) {
        long startTime = System.nanoTime(); // Засекаем время начала обработки
        List<String> errors = new ArrayList<>();
        Question randomQuestion = null;

        try {
            // Получаем все вопросы
            List<Question> questions = questionRepository.findAll();
            if (!questions.isEmpty()) {
                // Выбираем случайный вопрос
                Random random = new Random();
                randomQuestion = questions.get(random.nextInt(questions.size()));
            } else {
                errors.add("Вопросы отсутствуют в базе данных");
            }
        } catch (Exception ex) {
            errors.add("Ошибка при получении случайного вопроса: " + ex.getMessage());
        }

        String user = SecurityContextHolder.getContext().getAuthentication().getName();

        // Формируем ответ
        ApiResponse<Question> response = ApiResponse.<Question>builder()
                .timestamp(LocalDateTime.now())
                .status(errors.isEmpty() ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(errors.isEmpty() ? "Случайный вопрос успешно получен" : "Ошибки при получении данных")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .processingTime(calculateProcessingTime(startTime))
                .user(user)
                .data(randomQuestion)
                .count(randomQuestion == null ? 0 : 1)
                .errors(errors.isEmpty() ? null : errors)
                .build();

        return ResponseEntity.status(errors.isEmpty() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    @Operation(summary = "Ответить на вопрос")
    @PostMapping("/submit-answer")
    public ResponseEntity<ApiResponse<Integer>> submitAnswer(
            @RequestParam Long questionId,
            @RequestParam String userAnswer,
            HttpServletRequest request) {
        long startTime = System.nanoTime(); // Засекаем время начала обработки
        List<String> errors = new ArrayList<>();
        Integer updatedPoints = null;

        try {
            // Получаем текущего пользователя
            String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

            // Получаем пользователя из базы данных
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Ищем вопрос по ID
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Вопрос не найден"));

            // Проверяем правильность ответа
            if (question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim())) {
                // Увеличиваем очки пользователя
                user.setPoints(user.getPoints() + 10); // Например, 10 очков за правильный ответ
                userRepository.save(user);
                updatedPoints = user.getPoints();
            } else {
                errors.add("Ответ неверный");
            }
        } catch (Exception ex) {
            errors.add("Ошибка при обработке ответа: " + ex.getMessage());
        }

        // Формируем ответ
        ApiResponse<Integer> response = ApiResponse.<Integer>builder()
                .timestamp(LocalDateTime.now())
                .status(errors.isEmpty() ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(errors.isEmpty() ? "Ответ обработан успешно" : "Ошибки при обработке ответа")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .processingTime(calculateProcessingTime(startTime))
                .user(SecurityContextHolder.getContext().getAuthentication().getName())
                .data(updatedPoints)
                .errors(errors.isEmpty() ? null : errors)
                .build();

        return ResponseEntity.status(errors.isEmpty() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String calculateProcessingTime(long startTime) {
        long endTime = System.nanoTime();
        long durationInMs = (endTime - startTime) / 1_000_000;
        return durationInMs + " ms";
    }

}
