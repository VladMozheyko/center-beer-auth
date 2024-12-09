package fr.mossaab.security.controller;

import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.ErrorRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.service.ErrorService;
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

    private final ErrorService errorService;

    @Operation(summary = "Поиск ошибки по идентификатору")
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<ErrorService.ErrorDto> getErrorById(@PathVariable Long id) {
        return errorService.getErrorById(id);
    }

    @Operation(summary = "Вывод всех ошибок")
    @GetMapping("/get-all")
    public ResponseEntity<List<ErrorService.ErrorDto>> getAllErrors() {
        return errorService.getAllErrors();
    }

    @Operation(summary = "Создание новой ошибки")
    @PostMapping("/add")
    public ResponseEntity<ErrorService.ErrorDto> createError(@RequestBody ErrorService.CreateErrorDto createErrorDto) {
        return errorService.createError(createErrorDto);
    }

    @Operation(summary = "Обновление полей ошибки")
    @PutMapping("/update/{id}")
    public ResponseEntity<ErrorService.ErrorDto> updateError(
            @PathVariable Long id,
            @RequestBody ErrorService.UpdateErrorDto updateErrorDto) {
        return errorService.updateError(id, updateErrorDto);
    }

    @Operation(summary = "Удаление ошибки по идентификатору с отвязкой от серии")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteError(@PathVariable Long id) {
        return errorService.deleteError(id);
    }

}