package fr.mossaab.security.controller;
import fr.mossaab.security.entities.Type;
import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.repository.KindRepository;
import fr.mossaab.security.repository.TypeRepository;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Типы", description = "API для работы с типами")
@RestController
@SecurityRequirements
@RequiredArgsConstructor
@RequestMapping("/type")
public class TypeController {

    private final TypeRepository typeRepository;
    private final KindRepository kindRepository;

    @Operation(summary = "Поиск типа по идентификатору")
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<TypeDto> getTypeById(@PathVariable Long id) {
        Optional<Type> typeOptional = typeRepository.findById(id);
        if (typeOptional.isPresent()) {
            Type type = typeOptional.get();
            TypeDto typeDto = convertToDto(type);
            return ResponseEntity.ok(typeDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Поиск типа по названию")
    @GetMapping("/find-by-title/{title}")
    public ResponseEntity<TypeDto> getTypeByTitle(@PathVariable String title) {
        Optional<Type> typeOptional = typeRepository.findByTitle(title);
        if (typeOptional.isPresent()) {
            Type type = typeOptional.get();
            TypeDto typeDto = convertToDto(type);
            return ResponseEntity.ok(typeDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Вывод всех типов")
    @GetMapping("/get-all")
    public ResponseEntity<List<TypeDto>> getAllTypes() {
        List<Type> types = typeRepository.findAll();
        List<TypeDto> typeDtos = new ArrayList<>();

        for (Type type : types) {
            typeDtos.add(convertToDto(type));
        }

        return ResponseEntity.ok(typeDtos);
    }

    @Operation(summary = "Создание нового типа")
    @PostMapping("/add")
    public ResponseEntity<TypeDto> createType(@RequestBody CreateTypeDto createTypeDto) {
        Type type = new Type();
        type.setTitle(createTypeDto.getTitle());
        type.setDescription(createTypeDto.getDescription());
        type = typeRepository.save(type);

        TypeDto typeDto = convertToDto(type);
        return ResponseEntity.status(HttpStatus.CREATED).body(typeDto);
    }

    @Operation(summary = "Обновление полей типа")
    @PutMapping("/update/{id}")
    public ResponseEntity<TypeDto> updateType(
            @PathVariable Long id,
            @RequestBody UpdateTypeDto updateTypeDto
    ) {
        Optional<Type> typeOptional = typeRepository.findById(id);
        if (typeOptional.isPresent()) {
            Type type = typeOptional.get();
            if (updateTypeDto.getTitle() != null) {
                type.setTitle(updateTypeDto.getTitle());
            }
            if (updateTypeDto.getDescription() != null) {
                type.setDescription(updateTypeDto.getDescription());
            }
            if (updateTypeDto.getKindId() != null) {
                Optional<Kind> kindOptional = kindRepository.findById(updateTypeDto.getKindId());
                if (kindOptional.isPresent()) {
                    Kind kind = kindOptional.get();
                    if (type.getKinds() == null) {
                        type.setKinds(new ArrayList<>());
                    }
                    type.getKinds().add(kind);
                }
            }
            type = typeRepository.save(type);

            TypeDto typeDto = convertToDto(type);
            return ResponseEntity.ok(typeDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Удаление типа по идентификатору")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteTypeById(@PathVariable Long id) {
        if (typeRepository.existsById(id)) {
            typeRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Удаление типа по названию")
    @DeleteMapping("/delete-by-title/{title}")
    public ResponseEntity<Void> deleteTypeByTitle(@PathVariable String title) {
        Optional<Type> typeOptional = typeRepository.findByTitle(title);
        if (typeOptional.isPresent()) {
            typeRepository.delete(typeOptional.get());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // Helper method to convert Type to TypeDto
    private TypeDto convertToDto(Type type) {
        List<Long> kindIds = new ArrayList<>();
        if (type.getKinds() != null) {
            for (Kind kind : type.getKinds()) {
                kindIds.add(kind.getId());
            }
        }
        return new TypeDto(type.getId(), type.getTitle(), type.getDescription(), kindIds);
    }

    // DTO Classes
    @Data
    @AllArgsConstructor
    public static class TypeDto {
        private Long id;
        private String title;
        private String description;
        private List<Long> kindIds;
    }

    @Data
    public static class CreateTypeDto {
        private String title;
        private String description;
    }

    @Data
    public static class UpdateTypeDto {
        private String title;
        private String description;
        private Long kindId; // ID of the Kind to be added to the Type
    }
}