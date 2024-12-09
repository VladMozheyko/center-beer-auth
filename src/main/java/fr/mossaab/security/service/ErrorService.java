package fr.mossaab.security.service;

import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.repository.ErrorRepository;
import fr.mossaab.security.repository.SeriesRepository;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ErrorService {

    private final ErrorRepository errorRepository;
    private final SeriesRepository seriesRepository;

    public ResponseEntity<ErrorDto> getErrorById(Long id) {
        Optional<Error> errorOptional = errorRepository.findById(id);
        if (errorOptional.isPresent()) {
            Error error = errorOptional.get();
            ErrorDto errorDto = mapToDto(error);
            return ResponseEntity.ok(errorDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    public ResponseEntity<List<ErrorDto>> getAllErrors() {
        List<Error> errors = errorRepository.findAll();
        List<ErrorDto> errorDtos = new ArrayList<>();
        for (Error error : errors) {
            errorDtos.add(mapToDto(error));
        }
        return ResponseEntity.ok(errorDtos);
    }

    public ResponseEntity<ErrorDto> createError(CreateErrorDto createErrorDto) {
        Error error = new Error();
        error.setCode(createErrorDto.getCode());
        error.setCause(createErrorDto.getCause());
        error.setDescription(createErrorDto.getDescription());
        error = errorRepository.save(error);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(error));
    }

    public ResponseEntity<ErrorDto> updateError(Long id, UpdateErrorDto updateErrorDto) {
        Optional<Error> errorOptional = errorRepository.findById(id);
        if (errorOptional.isPresent()) {
            Error error = errorOptional.get();
            if (updateErrorDto.getCode() != null) error.setCode(updateErrorDto.getCode());
            if (updateErrorDto.getCause() != null) error.setCause(updateErrorDto.getCause());
            if (updateErrorDto.getDescription() != null) error.setDescription(updateErrorDto.getDescription());
            if (updateErrorDto.getSeriesId() != null) {
                Optional<Series> seriesOptional = seriesRepository.findById(updateErrorDto.getSeriesId());
                seriesOptional.ifPresent(error::setSeries);
            }
            error = errorRepository.save(error);
            return ResponseEntity.ok(mapToDto(error));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    public ResponseEntity<Void> deleteError(Long id) {
        Optional<Error> errorOptional = errorRepository.findById(id);
        if (errorOptional.isPresent()) {
            Error error = errorOptional.get();
            if (error.getSeries() != null) {
                error.setSeries(null);
                errorRepository.save(error);
            }
            errorRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private ErrorDto mapToDto(Error error) {
        return new ErrorDto(
                error.getId(),
                error.getCode(),
                error.getCause(),
                error.getDescription(),
                error.getSeries() != null ? error.getSeries().getId() : null
        );
    }


    // DTO Classes
    @Data
    @AllArgsConstructor
    public static class ErrorDto {
        private Long id;
        @Schema(example = "Е1")
        private String code;
        @Schema(example = """
                Нет подачи газа или не открыт газовый кран;
                Неисправны электроды розжига;
                Неисправность газового клапана;
                Пониженное давление газа;
                Неисправность датчика контроля пламени;
                Выход из строя платы управления.
                """)
        private String cause;
        @Schema(example = """
                Неполадки, связанные с 
                неудачным розжигом. Котел не 
                работает.
                """)
        private String description;
        private Long seriesId;
    }

    @Data
    public static class CreateErrorDto {
        @Schema(example = "Е1")
        private String code;
        @Schema(example = """
                Нет подачи газа или не открыт газовый кран;
                Неисправны электроды розжига;
                Неисправность газового клапана;
                Пониженное давление газа;
                Неисправность датчика контроля пламени;
                Выход из строя платы управления.
                """)
        private String cause;
        @Schema(example = """
                Неполадки, связанные с 
                неудачным розжигом. Котел не 
                работает.
                """)
        private String description;
    }

    @Data
    public static class UpdateErrorDto {
        @Schema(example = "Е1", nullable = true)
        private String code;
        @Schema(example = """
                Нет подачи газа или не открыт газовый кран;
                Неисправны электроды розжига;
                Неисправность газового клапана;
                Пониженное давление газа;
                Неисправность датчика контроля пламени;
                Выход из строя платы управления.
                """, nullable = true)
        private String cause;
        @Schema(example = """
                Неполадки, связанные с 
                неудачным розжигом. Котел не 
                работает.
                """, nullable = true)
        private String description;
        @Schema(nullable = true)
        private Long seriesId;
    }
}
