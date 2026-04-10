package fr.mossaab.security.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@Schema(description = "Локация пользователя")
public class LocationDto {

    @Schema(description = "Широта", example = "55.7558")
    @Min(value = -90, message = "Широта не может быть меньше -90")
    @Max(value = 90, message = "Широта не может быть больше 90")
    @NotNull(message = "Координата широты не должна быть пустой")
    private Double latitude;

    @Schema(description = "Долгота", example = "37.6173")
    @Min(value = -180, message = "Долгота не может быть меньше -180")
    @Max(value = 180, message = "Долгота не может быть больше 180")
    @NotNull(message = "Координата долготы не должна быть пустой")
    private Double longitude;

    @Schema(description = "Страна", example = "Россия")
    @Size(max = 100, message = "Название страны не может быть длиннее 100 символов")
    @Nullable
    private String country;

    @Schema(description = "Город", example = "Москва")
    @Size(max = 100, message = "Название города не может быть длиннее 100 символов")
    @Nullable
    private String city;
}