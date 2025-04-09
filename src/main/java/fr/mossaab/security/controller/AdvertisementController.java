package fr.mossaab.security.controller;

import fr.mossaab.security.dto.advertisement.AdvertisementResponse;
import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.repository.AdvertisementRepository;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(
        name = "Реклама",
        description = "Управление рекламными объявлениями: создание, просмотр, сортировка и получение данных файлов."
)
@RestController
@RequestMapping("/advertisements")
@SecurityRequirements()
@RequiredArgsConstructor
public class AdvertisementController {
    private final FileDataRepository fileDataRepository;
    private final AdvertisementRepository advertisementRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;


    @Operation(summary = "Получить рекламу по идентификатору", description = "Возвращает рекламное объявление по его ID.")
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<AdvertisementResponse> getAdvertisementById(@PathVariable("id") Long adId) {
        Advertisement advertisement = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("Реклама с ID " + adId + " не найдена"));

        AdvertisementResponse response = AdvertisementResponse.builder()
                .position(0) // Позиция не используется для одного объекта
                .cost(advertisement.getCost())
                .nickname(advertisement.getUser().getNickname())
                .fileDataId(advertisement.getFileData() != null ? advertisement.getFileData().getId() : null)
                .link(advertisement.getLink())
                .build();

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Получение идентификатора fileData рекламы с наибольшей стоимостью")
    @GetMapping("/advertisement-max-cost-file")
    public ResponseEntity<Long> getFileDataIdOfMaxCostAdvertisement() {
        Optional<Advertisement> maxCostAdvertisement = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getFileData() != null)
                .max(Comparator.comparingInt(Advertisement::getCost));

        if (maxCostAdvertisement.isEmpty()) {
            throw new RuntimeException("Не найдено реклам, связанных с файлами.");
        }
        FileData fileData = maxCostAdvertisement.get().getFileData();
        return ResponseEntity.ok(fileData.getId());
    }

    @Operation(
            summary = "Получение рекламы по убыванию стоимости с выводом идентификатора изображения",
            description = "Возвращает список рекламы, отсортированный по убыванию стоимости. Реклама фильтруется по переданному параметру status, " +
                    "значение которого должно соответствовать одному из значений AdvertisementStatus (например, APPROVED, PENDING, REJECTED)."
    )
    @GetMapping("/advertisements-by-cost")
    public ResponseEntity<List<AdvertisementResponse>> getAdvertisementsByCost(@RequestParam AdvertisementStatus status) {
        List<Advertisement> advertisements = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getStatus() == status)
                .collect(Collectors.toList());

        advertisements.sort((a1, a2) -> Integer.compare(a2.getCost(), a1.getCost()));

        List<AdvertisementResponse> response = new ArrayList<>();
        int position = 1;
        for (Advertisement ad : advertisements) {
            AdvertisementResponse adResponse = AdvertisementResponse.builder()
                    .id(ad.getId()) // <-- добавлено
                    .position(position)
                    .cost(ad.getCost())
                    .nickname(ad.getUser().getNickname())
                    .fileDataId(ad.getFileData() != null ? ad.getFileData().getId() : null)
                    .link(ad.getLink())
                    .build();
            response.add(adResponse);
            position++;
        }
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Создание рекламы с файлом",
            description = "Создает новое рекламное объявление с указанными параметрами. Если файл (изображение) предоставлен, загружает его. " +
                    "Возвращает сообщение об успешном создании с идентификатором созданной рекламы."
    )
    @PostMapping(value = "/add-advertisements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createAdvertisement(
            @Parameter(description = "Название рекламы")
            @RequestParam(name = "title") String title,
            @Parameter(description = "Ссылка на рекламируемый ресурс")
            @RequestParam(name = "link", required = false) String link,

            @Parameter(description = "Описание рекламы")
            @RequestParam(name = "description") String description,

            @Parameter(description = "Файл (изображение)")
            @RequestPart(name = "file", required = false) MultipartFile file,

            @Parameter(description = "Стоимость")
            @RequestParam(name = "cost") Integer cost
    ) throws IOException {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Найденная почта пользователя: " + userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getPears() < cost) {
            throw new RuntimeException("Недостаточно груш для создания рекламы. Требуется: " + cost + ", доступно: " + user.getPears());
        }

        Optional<Advertisement> currentLeader = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.LEADING)
                .findFirst();

        if (currentLeader.isPresent()) {
            int currentBid = currentLeader.get().getCost();
            if (cost < currentBid) {
                throw new RuntimeException("Ваша ставка " + cost + " меньше текущей: " + currentBid);
            }
            // Равная ставка разрешена — реклама попадёт в очередь
        }

        user.setPears(user.getPears() - cost);
        userRepository.save(user);

        Advertisement advertisement = Advertisement.builder()
                .title(title)
                .description(description)
                .createdAt(LocalDateTime.now())
                .cost(cost)
                .status(AdvertisementStatus.PENDING)
                .user(user)
                .link(link)
                .build();

        if (file != null && !file.isEmpty()) {
            FileData uploadImage = (FileData) storageService.uploadImageToFileSystem(file, advertisement);
            fileDataRepository.save(uploadImage);
            advertisement.setFileData(uploadImage);
        }
        advertisementRepository.save(advertisement);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Реклама успешно опубликована и добавлена в список. Идентификатор: " + advertisement.getId());
    }



}
