package fr.mossaab.security.controller;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Series;
import fr.mossaab.security.entities.Type;
import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.KindRepository;
import fr.mossaab.security.repository.TypeRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final FileDataRepository fileDataRepository;
    private final StorageService storageService;
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

    @Operation(summary = "Подгрузка фотографий типов")
    @PostMapping(value = "/add-image-for-type", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createSeries(@RequestPart String name, @RequestPart MultipartFile image) throws IOException {
        FileData uploadImage = (FileData) storageService.uploadImageToFileSystemWithName(image,name);
        fileDataRepository.save(uploadImage);
        return ResponseEntity.ok("Фотография для типа подгружена");
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
            if (updateTypeDto.getKindIds() != null && !updateTypeDto.getKindIds().isEmpty()) {
                for (Long kindId : updateTypeDto.getKindIds()) {
                    Optional<Kind> kindOptional = kindRepository.findById(kindId);
                    if (kindOptional.isPresent()) {
                        Kind kind = kindOptional.get();
                        if (type.getKinds() == null) {
                            type.setKinds(new ArrayList<>());
                        }
                        if (!type.getKinds().contains(kind)) {
                            type.getKinds().add(kind);
                        }
                    }
                }
            }
            type = typeRepository.save(type);

            TypeDto typeDto = convertToDto(type);
            return ResponseEntity.ok(typeDto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Operation(summary = "Удаление типа по идентификатору с разрывом связей")
    @DeleteMapping("/delete-by-id/{id}")
    public ResponseEntity<Void> deleteTypeById(@PathVariable Long id) {
        Optional<Type> typeOptional = typeRepository.findById(id);
        if (typeOptional.isPresent()) {
            Type type = typeOptional.get();

            // Разрыв связей с Kind
            if (type.getKinds() != null) {
                for (Kind kind : type.getKinds()) {
                    kind.setType(null);
                }
                type.getKinds().clear();
            }

            // Удаление типа
            typeRepository.delete(type);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


    @Operation(summary = "Удаление типа по названию с разрывом связей")
    @DeleteMapping("/delete-by-title/{title}")
    public ResponseEntity<Void> deleteTypeByTitle(@PathVariable String title) {
        Optional<Type> typeOptional = typeRepository.findByTitle(title);
        if (typeOptional.isPresent()) {
            Type type = typeOptional.get();

            // Разрыв связей с Kind
            if (type.getKinds() != null) {
                for (Kind kind : type.getKinds()) {
                    kind.setType(null);
                }
                type.getKinds().clear();
            }

            // Удаление типа
            typeRepository.delete(type);

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
        @Schema(example = "TOIVO")
        private String title;
        @Schema(example = "Котлы настенные газовые")
        private String description;
        private List<Long> kindIds;
    }

    @Data
    public static class CreateTypeDto {
        @Schema(example = "TOIVO")
        private String title;
        @Schema(example = "Котлы настенные газовые")
        private String description;
    }

    @Data
    public static class UpdateTypeDto {
        @Schema(example = "TOIVO", nullable = true)
        private String title;
        @Schema(example = "Котлы настенные газовые", nullable = true)
        private String description;
        @Schema(nullable = true)
        private List<Long> kindIds; // Список ID Kind для добавления к Type
    }
}