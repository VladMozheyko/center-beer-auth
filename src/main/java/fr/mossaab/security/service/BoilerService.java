package fr.mossaab.security.service;

import fr.mossaab.security.entities.Boiler;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Value;
import fr.mossaab.security.repository.BoilerRepository;
import fr.mossaab.security.repository.SeriesRepository;
import fr.mossaab.security.repository.ValueRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoilerService {

    private final BoilerRepository boilerRepository;
    private final SeriesRepository seriesRepository;
    private final ValueRepository valueRepository;

    public List<BoilerDTO> getAllBoilers() {
        List<Boiler> boilers = boilerRepository.findAll();
        List<BoilerDTO> boilerDTOs = new ArrayList<>();
        for (Boiler boiler : boilers) {
            boilerDTOs.add(new BoilerDTO(boiler));
        }
        return boilerDTOs;
    }

    public ResponseEntity<BoilerDTO> getBoilerById(Long id) {
        Optional<Boiler> boilerOptional = boilerRepository.findById(id);
        return boilerOptional.map(boiler -> ResponseEntity.ok(new BoilerDTO(boiler)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ResponseEntity<BoilerDTO> getBoilerByBarcode(Long barcode) {
        Optional<Boiler> boilerOptional = boilerRepository.findByBarcode(barcode);
        return boilerOptional.map(boiler -> ResponseEntity.ok(new BoilerDTO(boiler)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ResponseEntity<BoilerDTO> createBoiler(BoilerCreateDTO boilerCreateDTO) {
        Boiler boiler = new Boiler();
        boiler.setNumber(boilerCreateDTO.getNumber());
        boiler.setBarcode(boilerCreateDTO.getBarcode());
        boiler = boilerRepository.save(boiler);
        return new ResponseEntity<>(new BoilerDTO(boiler), HttpStatus.CREATED);
    }

    public ResponseEntity<BoilerDTO> updateBoiler(Long id, BoilerUpdateDTO boilerDTO, Long seriesId, List<Long> valueIds) {
        Optional<Boiler> boilerOptional = boilerRepository.findById(id);
        if (boilerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Boiler boiler = boilerOptional.get();

        if (boilerDTO.getNumber() != null) boiler.setNumber(boilerDTO.getNumber());
        if (boilerDTO.getBarcode() != null) boiler.setBarcode(boilerDTO.getBarcode());

        if (seriesId != null) {
            Optional<Series> seriesOptional = seriesRepository.findById(seriesId);
            seriesOptional.ifPresent(boiler::setSeries);
        }

        if (valueIds != null) {
            List<Value> values = new ArrayList<>();
            for (Long valueId : valueIds) {
                Optional<Value> valueOptional = valueRepository.findById(valueId);
                valueOptional.ifPresent(values::add);
            }
            boiler.setValues(values);
        }

        boiler = boilerRepository.save(boiler);
        return ResponseEntity.ok(new BoilerDTO(boiler));
    }

    public ResponseEntity<Void> deleteBoilerById(Long id) {
        Optional<Boiler> boilerOptional = boilerRepository.findById(id);
        if (boilerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Boiler boiler = boilerOptional.get();

        if (boiler.getSeries() != null) {
            boiler.getSeries().getBoilers().remove(boiler);
            seriesRepository.save(boiler.getSeries());
        }

        if (!boiler.getValues().isEmpty()) {
            for (Value value : boiler.getValues()) {
                value.getBoilers().remove(boiler);
                valueRepository.save(value);
            }
        }

        boilerRepository.delete(boiler);
        return ResponseEntity.noContent().build();
    }


    @Getter
    @Setter
    public static class BoilerCreateDTO {
        @Schema(example = "100")
        private Long number;
        @Schema(example = "4640270282360")
        private Long barcode;
    }

    // DTO for Boiler
    @Getter
    @Setter
    public static class BoilerUpdateDTO {
        @Schema(example = "100", nullable = true)
        private Long number;
        @Schema(example = "4640270282360", nullable = true)
        private Long barcode;
        @Schema(nullable = true)
        private Long seriesId;
        @Schema(nullable = true)
        private List<Long> valueIds;

        // Constructor to map Boiler entity to DTO
    }

    @Getter
    @Setter
    public static class BoilerDTO {
        private Long id;
        @Schema(example = "100")
        private Long number;
        @Schema(example = "4640270282360")
        private Long barcode;
        private Long seriesId;
        private List<Long> valueIds;

        // Constructor to map Boiler entity to DTO
        public BoilerDTO(Boiler boiler) {
            this.id = boiler.getId();
            this.number = boiler.getNumber();
            this.barcode = boiler.getBarcode();
            this.seriesId = boiler.getSeries() != null ? boiler.getSeries().getId() : null;
            this.valueIds = new ArrayList<>();
            for (Value value : boiler.getValues()) {
                this.valueIds.add(value.getId());
            }
        }
    }
}
