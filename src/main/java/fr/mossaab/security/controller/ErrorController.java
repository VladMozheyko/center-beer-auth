package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.ErrorRepository;
import fr.mossaab.security.repository.SeriesRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Tag(name = "Ошибки", description = "API для работы с ошибками")
@RestController
@SecurityRequirements
@RequiredArgsConstructor
@RequestMapping("/error")
public class ErrorController {

    private final ErrorRepository errorRepository;
    private final SeriesRepository seriesRepository;

    @Operation(summary = "Поиск ошибки по идентификатору")
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<ErrorDto> getErrorById(@PathVariable Long id) {
        Optional<Error> errorOptional = errorRepository.findById(id);
        if (errorOptional.isPresent()) {
            Error error = errorOptional.get();
            ErrorDto errorDto = new ErrorDto(
                    error.getId(),
                    error.getCode(),
                    error.getCause(),
                    error.getDescription(),
                    error.getSeries() != null ? error.getSeries().getId() : null
            );
            return ResponseEntity.ok(errorDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Вывод всех ошибок")
    @GetMapping("/get-all")
    public ResponseEntity<List<ErrorDto>> getAllErrors() {
        List<Error> errors = errorRepository.findAll();
        List<ErrorDto> errorDtos = new ArrayList<>();

        for (Error error : errors) {
            errorDtos.add(new ErrorDto(
                    error.getId(),
                    error.getCode(),
                    error.getCause(),
                    error.getDescription(),
                    error.getSeries() != null ? error.getSeries().getId() : null
            ));
        }

        return ResponseEntity.ok(errorDtos);
    }

    @Operation(summary = "Создание новой ошибки")
    @PostMapping("/add")
    public ResponseEntity<ErrorDto> createError(@RequestBody CreateErrorDto createErrorDto) {
        Error error = new Error();
        error.setCode(createErrorDto.getCode());
        error.setCause(createErrorDto.getCause());
        error.setDescription(createErrorDto.getDescription());
        error = errorRepository.save(error);

        ErrorDto errorDto = new ErrorDto(
                error.getId(),
                error.getCode(),
                error.getCause(),
                error.getDescription(),
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(errorDto);
    }

    @Operation(summary = "Обновление полей ошибки")
    @PutMapping("/update/{id}")
    public ResponseEntity<ErrorDto> updateError(
            @PathVariable Long id,
            @RequestBody UpdateErrorDto updateErrorDto
    ) {
        Optional<Error> errorOptional = errorRepository.findById(id);
        if (errorOptional.isPresent()) {
            Error error = errorOptional.get();
            if (updateErrorDto.getCode() != null) {
                error.setCode(updateErrorDto.getCode());
            }
            if (updateErrorDto.getCause() != null) {
                error.setCause(updateErrorDto.getCause());
            }
            if (updateErrorDto.getDescription() != null) {
                error.setDescription(updateErrorDto.getDescription());
            }
            if (updateErrorDto.getSeriesId() != null) {
                Optional<Series> seriesOptional = seriesRepository.findById(updateErrorDto.getSeriesId());
                if (seriesOptional.isPresent()) {
                    error.setSeries(seriesOptional.get());
                }
            }
            error = errorRepository.save(error);

            ErrorDto errorDto = new ErrorDto(
                    error.getId(),
                    error.getCode(),
                    error.getCause(),
                    error.getDescription(),
                    error.getSeries() != null ? error.getSeries().getId() : null
            );

            return ResponseEntity.ok(errorDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Удаление ошибки по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteError(@PathVariable Long id) {
        if (errorRepository.existsById(id)) {
            errorRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // DTO Classes
    @Data
    @AllArgsConstructor
    public static class ErrorDto {
        private Long id;
        @Schema(example = "Е1")
        private String code;
        @Schema(example = "Нет подачи газа или не открыт газовый кран;\n" +
                "Неисправны электроды розжига;\n" +
                "Неисправность газового\n" +
                "клапана;\n" +
                "Пониженное давление\n" +
                "газа;\n" +
                "Неисправность датчика\n" +
                "контроля пламени;\n" +
                "Выход из строя платы\n" +
                "управления.")
        private String cause;
        @Schema(example = "Неполадки, связанные с\n" + "неудачным розжигом. Котел не\n" +"работает")
        private String description;
        private Long seriesId;
    }

    @Data
    public static class CreateErrorDto {
        @Schema(example = "Е1")
        private String code;
        @Schema(example = "Нет подачи газа или не открыт газовый кран;\n" +
                "Неисправны электроды розжига;\n" +
                "Неисправность газового\n" +
                "клапана;\n" +
                "Пониженное давление\n" +
                "газа;\n" +
                "Неисправность датчика\n" +
                "контроля пламени;\n" +
                "Выход из строя платы\n" +
                "управления.")
        private String cause;
        @Schema(example = "Неполадки, связанные с\n" + "неудачным розжигом. Котел не\n" +"работает")
        private String description;
    }

    @Data
    public static class UpdateErrorDto {
        @Schema(example = "Е1")
        private String code;
        @Schema(example = "Нет подачи газа или не открыт газовый кран;\n" +
                "Неисправны электроды розжига;\n" +
                "Неисправность газового\n" +
                "клапана;\n" +
                "Пониженное давление\n" +
                "газа;\n" +
                "Неисправность датчика\n" +
                "контроля пламени;\n" +
                "Выход из строя платы\n" +
                "управления.")
        private String cause;
        @Schema(example = "Неполадки, связанные с\n" + "неудачным розжигом. Котел не\n" +"работает")
        private String description;
        private Long seriesId;
    }
}