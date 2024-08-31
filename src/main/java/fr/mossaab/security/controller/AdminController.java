package fr.mossaab.security.controller;

import fr.mossaab.security.dto.request.BarcodeTypeDto;
import fr.mossaab.security.dto.request.BarcodeTypeUpdateDto;
import fr.mossaab.security.dto.request.BarcodeUpdateDto;
import fr.mossaab.security.dto.request.UserEmailWithBoilerPhotoNamesDTO;
import fr.mossaab.security.dto.response.GetAllUsersResponse;
import fr.mossaab.security.dto.response.GetUsersDto;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import fr.mossaab.security.repository.BarcodeRepository;
import fr.mossaab.security.repository.BarcodeTypeRepository;
import fr.mossaab.security.repository.BonusRequestRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.impl.BarcodeService;
import fr.mossaab.security.service.impl.BarcodeSummaryDto;
import fr.mossaab.security.service.impl.BoilerPurchasePhotoService;
import fr.mossaab.security.service.impl.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Tag(name = "Admin", description = "Контроллер предоставляющие методы доступные пользователю с ролью администратор")
@RestController
@RequestMapping("/admin")
@SecurityRequirements()
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final BoilerPurchasePhotoService boilerPurchasePhotoService;
    private final BarcodeService barcodeService;
    private final BarcodeTypeRepository barcodeTypeRepository;
    private final BonusRequestRepository bonusRequestRepository;
    private final BarcodeRepository barcodeRepository;

    @Operation(summary = "Получить всех пользователей", description = "Этот эндпоинт возвращает список всех пользователей с пагинацией.")
    @GetMapping("/allUsers")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        List<User> users = userRepository.findAll();

        List<GetUsersDto> userDtos = new ArrayList<>();

        for (User user : users) {
            WorkerRole workerRole = user.getWorkerRoles();
            Role role = user.getRole();
            GetUsersDto userDto = new GetUsersDto(
                    user.getFirstname() != null ? user.getFirstname() : null,
                    user.getEmail() != null ? user.getEmail() : null,
                    user.getLastname() != null ? user.getLastname() : null,
                    user.getPhoneNumber() != null ? user.getPhoneNumber() : null,
                    workerRole != null ? workerRole.name() : null,
                    user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null,
                    "http://31.129.102.70:8080/user/fileSystem/"+(user.getFileData() != null && user.getFileData().getName() != null ? user.getFileData().getName() : null),
                    user.getActivationCode() == null,
                    role != null ? role.name() : null,
                    user.getId() != null ? user.getId().toString() : null
            );
            userDtos.add(userDto);
        }

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, userDtos.size());
        List<GetUsersDto> paginatedUserDtos = userDtos.subList(startIndex, endIndex);

        GetAllUsersResponse response = new GetAllUsersResponse();
        response.setStatus("success");
        response.setNotify("Пользователи получены");
        response.setUsers(paginatedUserDtos);
        response.setOffset(page + 1);
        response.setPageNumber(page);
        response.setTotalElements(userDtos.size());
        response.setTotalPages((int) Math.ceil((double) userDtos.size() / size));
        response.setPageSize(size);
        response.setLast((page + 1) * size >= userDtos.size());
        response.setFirst(page == 0);

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Получить по почтам все запросы, их статус, id, а также фотки котлов")
    @GetMapping("/print-all-bonus-program-photos")
    public ResponseEntity<List<Map<String, Object>>> printPhotos() {
        // Получаем всех пользователей
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        // Проходим по каждому пользователю
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("email", user.getEmail());

            List<Map<String, Object>> bonusRequestList = new ArrayList<>();

            // Получаем все бонусные запросы пользователя
            for (BonusRequest bonusRequest : user.getBonusRequests()) {
                Map<String, Object> bonusRequestMap = new HashMap<>();
                bonusRequestMap.put("bonusRequestId", bonusRequest.getId());

                // Добавляем статус бонусного запроса
                bonusRequestMap.put("status", bonusRequest.getStatus().name());

                // Добавляем сообщение об отказе, если статус REJECTED
                if (bonusRequest.getStatus() == BonusRequest.RequestStatus.REJECTED) {
                    bonusRequestMap.put("rejectionMessage", bonusRequest.getRejectionMessage());
                }

                List<String> photoNames = new ArrayList<>();
                // Получаем все фотографии покупки котла для этого бонусного запроса
                for (BoilerPurchasePhoto photo : bonusRequest.getBoilerPurchasePhotos()) {
                    photoNames.add("http://31.129.102.70:8080/admin/fileSystem/" + photo.getName());
                }

                bonusRequestMap.put("photos", photoNames);
                bonusRequestList.add(bonusRequestMap);
            }

            userMap.put("bonusRequests", bonusRequestList);
            result.add(userMap);
        }

        // Возвращаем результат в виде JSON
        return ResponseEntity.ok(result);
    }

    @PostMapping("/updateStatus")
    @Operation(summary = "Обновление статуса запроса пользователя на получение баллов за бонусную программу")
    public ResponseEntity<String> updateBonusRequestStatus(
            @RequestParam("requestId") Long requestId,
            @RequestParam("status") BonusRequest.RequestStatus status,
            @RequestParam(value = "rejectionMessage", required = false) String rejectionMessage) {

        // Поиск BonusRequest по ID
        var bonusRequest = bonusRequestRepository.findById(requestId).orElse(null);
        if (bonusRequest == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("BonusRequest not found.");
        }

        // Установка статуса и сообщения об отказе, если статус REJECTED
        bonusRequest.setStatus(status);
        if (status == BonusRequest.RequestStatus.REJECTED && rejectionMessage != null) {
            bonusRequest.setRejectionMessage(rejectionMessage);
            bonusRequest.getBarcode().setBonusRequest(null);
        }
        if (status == BonusRequest.RequestStatus.APPROVED) {
            bonusRequest.getBarcode().setUsed(true);
            bonusRequest.getUser().setBalance(bonusRequest.getUser().getBalance()+bonusRequest.getBarcode().getBarcodeType().getPoints());
        }
        // Сохранение обновленного BonusRequest
        bonusRequestRepository.save(bonusRequest);

        return ResponseEntity.ok("BonusRequest status updated successfully.");
    }
    @Operation(summary = "Загрузка изображения фотографии присланной пользователем в рамках бонусной программы", description = "Этот эндпоинт позволяет загрузить изображение присланное пользователем в рамках бонусной программы")
    @GetMapping("/fileSystem/{fileName}")
    public ResponseEntity<?> downloadImageFromBonusProgramFromFileSystem(@PathVariable String fileName) throws IOException {
        byte[] imageData = boilerPurchasePhotoService.downloadImageFromFileSystem(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);
    }
    // Добавление нового штрих-кода
    @PostMapping("/add-barcode")
    @Operation(summary = "Добавление нового штрих-кода")
    public ResponseEntity<Barcode> addBarcode(@RequestParam Long code, @RequestParam Long barcodeTypeId) {
        Barcode createdBarcode = barcodeService.addBarcode(code, barcodeTypeId);
        return ResponseEntity.ok(createdBarcode);
    }
    @Operation(summary = "Добавление нового типа штрих-кода")
    @PostMapping("/add-barcode-type")
    public ResponseEntity<BarcodeType> addBarcodeType(@RequestParam("points") int points,
                                                      @RequestParam("type")  String type,
                                                      @RequestParam("subtype") String subtype) {
        BarcodeType barcodeType = new BarcodeType();
        barcodeType.setPoints(points);
        barcodeType.setType(type);
        barcodeType.setSubtype(subtype);

        BarcodeType savedBarcodeType = barcodeTypeRepository.save(barcodeType);

        // Возвращаем ResponseEntity с объектом и статусом 201 Created
        return new ResponseEntity<>(savedBarcodeType, HttpStatus.CREATED);
    }
    @Operation(summary = "Вывод всех штрих кодов")
    @GetMapping("/all-barcode")
    public ResponseEntity<List<BarcodeSummaryDto>> getAllBarcodesSummary() {
        List<Barcode> barcodes = barcodeRepository.findAll();
        List<BarcodeSummaryDto> barcodeSummaryDtos = new ArrayList<>();

        // Используем цикл for для обработки списка
        for (Barcode barcode : barcodes) {
            BarcodeSummaryDto summaryDto = new BarcodeSummaryDto(barcode.getId(), barcode.getCode(), barcode.isUsed());
            barcodeSummaryDtos.add(summaryDto);
        }

        return ResponseEntity.ok(barcodeSummaryDtos);
    }
    @Operation(summary = "Редактирование штрих-кода по его id")
    @PutMapping("update-barcode/{id}")
    public ResponseEntity<Barcode> updateBarcode(@PathVariable Long id,
                                                 @RequestBody BarcodeUpdateDto updateDto) {

        Optional<Barcode> existingBarcode = barcodeRepository.findById(id);
        if (existingBarcode.isPresent()) {
            Barcode barcode = existingBarcode.get();
            barcode.setCode(updateDto.getCode());
            barcode.setUsed(updateDto.isUsed());
            return ResponseEntity.ok(barcodeRepository.save(barcode));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @Operation(summary = "Редактирование типа штрих-кода по его id")
    @PutMapping("update-barcode-type/{id}/type")
    public ResponseEntity<BarcodeType> updateBarcodeType(@PathVariable Long id,
                                                         @RequestBody BarcodeTypeUpdateDto updateDto) {

        Optional<BarcodeType> existingBarcodeType = barcodeTypeRepository.findById(id);
        if (existingBarcodeType.isPresent()) {
            BarcodeType barcodeType = existingBarcodeType.get();
            barcodeType.setPoints(updateDto.getPoints());
            barcodeType.setType(updateDto.getType());
            barcodeType.setSubtype(updateDto.getSubtype());
            return ResponseEntity.ok(barcodeTypeRepository.save(barcodeType));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
