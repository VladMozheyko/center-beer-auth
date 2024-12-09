package fr.mossaab.security.service;

import fr.mossaab.security.entities.Characteristic;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Unit;
import fr.mossaab.security.repository.CharacteristicRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.UnitRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CharacteristicService {

    private final CharacteristicRepository characteristicRepository;
    private final UnitRepository unitRepository;
    private final SeriesRepository seriesRepository;

    public List<CharacteristicDto> getAllCharacteristics() {
        List<Characteristic> characteristics = characteristicRepository.findAll();
        List<CharacteristicDto> result = new ArrayList<>();

        for (Characteristic characteristic : characteristics) {
            CharacteristicDto dto = new CharacteristicDto();
            dto.setId(characteristic.getId());
            dto.setTitle(characteristic.getTitle());

            List<Long> unitIds = new ArrayList<>();
            for (Unit unit : characteristic.getUnits()) {
                unitIds.add(unit.getId());
            }
            dto.setUnitIds(unitIds);

            List<Long> seriesIds = new ArrayList<>();
            for (Series series : characteristic.getSeries()) {
                seriesIds.add(series.getId());
            }
            dto.setSeriesIds(seriesIds);

            result.add(dto);
        }

        return result;
    }

    public Characteristic createCharacteristic(CharacteristicCreateDto characteristicCreateDto) {
        Characteristic characteristic = Characteristic.builder()
                .title(characteristicCreateDto.getTitle())
                .units(new ArrayList<>())
                .series(new ArrayList<>())
                .build();
        return characteristicRepository.save(characteristic);
    }

    public Characteristic updateCharacteristic(Long id, CharacteristicUpdateDto characteristicDto) {
        Optional<Characteristic> optionalCharacteristic = characteristicRepository.findById(id);
        if (optionalCharacteristic.isEmpty()) {
            throw new RuntimeException("Characteristic not found");
        }

        Characteristic characteristic = optionalCharacteristic.get();
        if (characteristicDto.getTitle() != null) {
            characteristic.setTitle(characteristicDto.getTitle());
        }

        if (characteristicDto.getUnitIds() != null && !characteristicDto.getUnitIds().isEmpty()) {
            for (Long unitId : characteristicDto.getUnitIds()) {
                Optional<Unit> unitOptional = unitRepository.findById(unitId);
                unitOptional.ifPresent(characteristic.getUnits()::add);
            }
        }

        if (characteristicDto.getSeriesIds() != null && !characteristicDto.getSeriesIds().isEmpty()) {
            for (Long seriesId : characteristicDto.getSeriesIds()) {
                Optional<Series> seriesOptional = seriesRepository.findById(seriesId);
                seriesOptional.ifPresent(characteristic.getSeries()::add);
            }
        }

        return characteristicRepository.save(characteristic);
    }

    public void deleteCharacteristic(Long id) {
        Optional<Characteristic> characteristicOptional = characteristicRepository.findById(id);
        if (characteristicOptional.isEmpty()) {
            throw new RuntimeException("Characteristic not found");
        }

        Characteristic characteristic = characteristicOptional.get();

        for (Unit unit : characteristic.getUnits()) {
            unit.getCharacteristics().remove(characteristic);
        }
        for (Series series : characteristic.getSeries()) {
            series.getCharacteristics().remove(characteristic);
        }

        characteristicRepository.delete(characteristic);
    }

    public CharacteristicDto getCharacteristicById(Long id) {
        Optional<Characteristic> optionalCharacteristic = characteristicRepository.findById(id);
        if (optionalCharacteristic.isEmpty()) {
            throw new RuntimeException("Characteristic not found");
        }

        Characteristic characteristic = optionalCharacteristic.get();
        CharacteristicDto dto = new CharacteristicDto();
        dto.setId(characteristic.getId());
        dto.setTitle(characteristic.getTitle());

        List<Long> unitIds = new ArrayList<>();
        for (Unit unit : characteristic.getUnits()) {
            unitIds.add(unit.getId());
        }
        dto.setUnitIds(unitIds);

        List<Long> seriesIds = new ArrayList<>();
        for (Series series : characteristic.getSeries()) {
            seriesIds.add(series.getId());
        }
        dto.setSeriesIds(seriesIds);

        return dto;
    }

    @Data
    public static class CharacteristicCreateDto {
        @Schema(example = "Макс./мин. тепловая мощность в режиме отопление")
        private String title;
    }

    // DTO для вывода данных
    @Data
    public static class CharacteristicDto {
        private Long id;
        @Schema(example = "Макс./мин. тепловая мощность в режиме отопление")
        private String title;
        private List<Long> unitIds;
        private List<Long> seriesIds;

        // Конструкторы, геттеры и сеттеры
    }

    @Data
    public static class CharacteristicUpdateDto {
        @Schema(example = "Макс./мин. тепловая мощность в режиме отопление", nullable = true)
        private String title;
        @Schema(nullable = true)
        private List<Long> unitIds;
        @Schema(nullable = true)
        private List<Long> seriesIds;
    }
}