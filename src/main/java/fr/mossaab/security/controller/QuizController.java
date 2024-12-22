package fr.mossaab.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
@Tag(name = "Викторина", description = "API для работы с викториной. Редактирование и удаление элементов осуществляется уже посредством google tables")

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private static final String CSV_URL = "https://docs.google.com/spreadsheets/d/1RU9Nl4ogjWftcVX76wlWgm0gCmFs9Z5xyUSCU3Uz6cc/export?format=csv";
    @Operation(summary = "Вывод полного банка вопросов и ответов викторины в JSON из excel файла")
    @GetMapping("/questions")
    public List<List<String>> getQuizQuestions() throws Exception {
        List<List<String>> questions = new ArrayList<>();

        // Установка соединения
        URL url = new URL(CSV_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Чтение CSV-данных
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Разделение строки на элементы (предполагается, что разделитель - запятая)
                String[] values = line.split(",");
                List<String> row = new ArrayList<>();
                for (String value : values) {
                    row.add(value.trim().replaceAll("\"", "")); // Удаляем кавычки
                }
                questions.add(row);
            }
        }

        return questions;
    }
    @Operation(summary = "Вывод указанного банка вопросов по категории")
    @GetMapping("/questions-by-category")
    public void getQuizQuestionsByCategory() throws Exception {

    }

}
