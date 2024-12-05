package fr.mossaab.security.controller;
import fr.mossaab.security.entities.Boiler;
import fr.mossaab.security.entities.Characteristic;
import fr.mossaab.security.entities.Value;
import fr.mossaab.security.repository.BoilerRepository;
import fr.mossaab.security.repository.CharacteristicRepository;
import fr.mossaab.security.repository.ValueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/value")
@Tag(name = "Допустимые значения", description = "API для работы с допустимыми значениями")
@RequiredArgsConstructor
public class ValueController {

    private final ValueRepository valueRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BoilerRepository boilerRepository;
    // DTO for Value creation (без идентификатора)

    // 1. Method to find a value by ID
    @Operation(summary = "Поиск по идентификатору", description = "Найти значение по ID и показать все поля и идентификаторы всех связных сущностей")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ValueDTO> getValueById(@PathVariable Long id) {
        Optional<Value> valueOptional = valueRepository.findById(id);
        if (valueOptional.isPresent()) {
            return ResponseEntity.ok(new ValueDTO(valueOptional.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 2. Method to get all values with their related entities
    @Operation(summary = "Вывод всех значений", description = "Вывести все значения, все поля и идентификаторы всех связных сущностей")
    @GetMapping("/find-all")
    public List<ValueDTO> getAllValues() {
        List<ValueDTO> valueDTOs = new ArrayList<>();
        List<Value> values = valueRepository.findAll();
        for (Value value : values) {
            valueDTOs.add(new ValueDTO(value));
        }
        return valueDTOs;
    }

    // 3. Method to delete a value by ID
    @Operation(summary = "Удаление значения", description = "Удалить значение по идентификатору, предварительно отвязав все связи")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteValueById(@PathVariable Long id) {
        // Проверяем, существует ли значение
        Value value = valueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Значение с ID " + id + " не найдено."));

        // Удаляем связь с характеристикой, если она существует
        if (value.getCharacteristic() != null) {
            Characteristic characteristic = value.getCharacteristic();
            value.setCharacteristic(null); // Убираем связь со стороны Value
            characteristicRepository.save(characteristic); // Сохраняем характеристику для фиксации изменений
        }

        // Удаляем связи с бойлерами
        value.getBoilers().forEach(boiler -> boiler.getValues().remove(value));
        value.getBoilers().clear();

        // Удаляем саму сущность
        valueRepository.delete(value);

        return ResponseEntity.noContent().build();
    }



    // 6. Method to create a new value (using CreateValueDTO without ID)
    @Operation(summary = "Создание нового значения", description = "Создать новое значение с генерируемым идентификатором")
    @PostMapping("/add")
    public ResponseEntity<ValueDTO> createValue(@RequestBody CreateValueDTO createValueDTO) {
        Value value = new Value();
        value.setSValue(createValueDTO.getSValue());
        value.setDValue(createValueDTO.getDValue());
        value.setMinValue(createValueDTO.getMinValue());
        value.setMaxValue(createValueDTO.getMaxValue());
        value = valueRepository.save(value);
        return new ResponseEntity<>(new ValueDTO(value), HttpStatus.CREATED);
    }

    // 7. Method to update a value and add an existing boiler or characteristic
    @Operation(summary = "Обновление значения", description = "Обновить поля значения и добавить существующие бойлеры и характеристики")
    @PutMapping("/update/{id}")
    public ResponseEntity<ValueDTO> updateValue(
            @PathVariable Long id,
            @RequestBody UpdateValueDTO valueDTO,
            @RequestParam(required = false) Long boilerId,
            @RequestParam(required = false) Long characteristicId) {

        Optional<Value> valueOptional = valueRepository.findById(id);
        if (valueOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Value value = valueOptional.get();

        // Обновляем поля, если они предоставлены
        if (valueDTO.getSValue() != null) value.setSValue(valueDTO.getSValue());
        if (valueDTO.getDValue() != null) value.setDValue(valueDTO.getDValue());
        if (valueDTO.getMinValue() != null) value.setMinValue(valueDTO.getMinValue());
        if (valueDTO.getMaxValue() != null) value.setMaxValue(valueDTO.getMaxValue());

        // Добавляем существующий бойлер в список, если ID предоставлен
        if (boilerId != null) {
            Optional<Boiler> boilerOptional = boilerRepository.findById(boilerId);
            if (boilerOptional.isPresent()) {
                Boiler boiler = boilerOptional.get();
                if (!value.getBoilers().contains(boiler)) {
                    value.getBoilers().add(boiler);
                }
            }
        }

        // Устанавливаем существующую характеристику, если ID предоставлен
        if (characteristicId != null) {
            Optional<Characteristic> characteristicOptional = characteristicRepository.findById(characteristicId);
            characteristicOptional.ifPresent(value::setCharacteristic);
        }

        value = valueRepository.save(value);
        return ResponseEntity.ok(new ValueDTO(value));
    }

    @Getter
    @Setter
    public static class CreateValueDTO {
        @Schema(example = "700х400х299")
        private String sValue;
        @Schema(example = "8.0")
        private Double dValue;
        @Schema(example = "4.0")
        private Double minValue;
        @Schema(example = "10.0")
        private Double maxValue;
    }

    // DTO for Value
    @Getter
    @Setter
    public static class ValueDTO {
        private Long id;
        private Long characteristicId;
        @Schema(example = "700х400х299")
        private String sValue;
        @Schema(example = "8.0")
        private Double dValue;
        @Schema(example = "4.0")
        private Double minValue;
        @Schema(example = "10.0")
        private Double maxValue;
        private List<Long> boilerIds;

        // Constructor to map Value entity to DTO
        public ValueDTO(Value value) {
            this.id = value.getId();
            this.characteristicId = value.getCharacteristic() != null ? value.getCharacteristic().getId() : null;
            this.sValue = value.getSValue();
            this.dValue = value.getDValue();
            this.minValue = value.getMinValue();
            this.maxValue = value.getMaxValue();
            this.boilerIds = new ArrayList<>();
            for (Boiler boiler : value.getBoilers()) {
                this.boilerIds.add(boiler.getId());
            }
        }
    }
    @Getter
    @Setter
    @Data
    public static class UpdateValueDTO {
        @Schema(nullable = true)
        private Long characteristicId;
        @Schema(example = "700х400х299", nullable = true)
        private String sValue;
        @Schema(example = "8.0", nullable = true)
        private Double dValue;
        @Schema(example = "4.0", nullable = true)
        private Double minValue;
        @Schema(example = "10.0", nullable = true)
        private Double maxValue;

    }
}
