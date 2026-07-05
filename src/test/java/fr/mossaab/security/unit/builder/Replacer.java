package fr.mossaab.security.unit.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@RequiredArgsConstructor
@Configuration
public class Replacer {

    private final ObjectMapper mapper;

    public String replaceLocalDateTimeToInstant (String json) throws JsonProcessingException {
        // Парсим JSON
        JsonNode rootNode = mapper.readTree(json);

        // Если есть lastIpAddress — обрабатываем createdAt внутри объектов массива
        if (rootNode.has("lastIpAddress") && rootNode.get("lastIpAddress").isArray()) {
            for (JsonNode item : rootNode.get("lastIpAddress")) {
                if (item.has("createdAt")) {
                    String isoString = item.get("createdAt").asText();
                    try {
                        // Пробуем распарсить как ISO-8601 — добавляем "Z", если нет суффикса
                        if (!isoString.endsWith("Z") && !isoString.contains("+") && !isoString.contains("-")) {
                            isoString += "Z"; // assume UTC
                        }
                        Instant instant = Instant.parse(isoString);
                        double timestamp = instant.getEpochSecond() + (instant.getNano() / 1_000_000_000.0);
                        ((ObjectNode) item).put("createdAt", timestamp);
                    } catch (Exception e) {
                        // Если не удалось — ставим текущее время как fallback
                        double fallback = Instant.now().getEpochSecond() +
                                (Instant.now().getNano() / 1_000_000_000.0);
                        ((ObjectNode) item).put("createdAt", fallback);
                    }
                }
            }
            return mapper.writeValueAsString(rootNode);
        }
        return json;
    }
}
