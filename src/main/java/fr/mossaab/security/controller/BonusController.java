package fr.mossaab.security.controller;

import fr.mossaab.security.dto.request.BarcodeTypeDto;
import fr.mossaab.security.dto.request.BarcodeTypeUpdateDto;
import fr.mossaab.security.dto.request.BarcodeUpdateDto;
import fr.mossaab.security.entities.Barcode;
import fr.mossaab.security.entities.BarcodeType;
import fr.mossaab.security.enums.RequestStatus;
import fr.mossaab.security.service.BarcodeService;
import fr.mossaab.security.dto.BarcodeSummaryDto;
import fr.mossaab.security.service.BonusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "Бонусная программа", description = "Контроллер предоставляющие методы контекста бонусной программы")
@RestController
@RequestMapping("/bonus-program")
@SecurityRequirements()
@RequiredArgsConstructor
public class BonusController {
    private final BarcodeService barcodeService;
    private final BonusService bonusService;

    @Operation(summary = "Позволяет получить все типы штрих кодов администратором")
    @GetMapping("/get-all-barcode-types")
    public ResponseEntity<List<BarcodeTypeDto>> getAllBarcodeTypesByAdmin() {
        return ResponseEntity.ok(bonusService.getAllBarcodeTypes());
    }


    @Operation(summary = "Позволяет подгрузить пользователем фотографии купленного продукта в бонусную программу")
    @PostMapping("/upload-photos")
    public ResponseEntity<String> uploadPhotosToBonusProgramByUser(@RequestParam("photos") List<MultipartFile> photos, @CookieValue("refresh-jwt-cookie") String cookie, @RequestParam("barcode-code") Long barcodeCode) throws IOException {
        return ResponseEntity.ok(bonusService.uploadPhotos(photos, cookie, barcodeCode));
    }

    @Operation(summary = "Позволяет подгрузить пользователем фотографии паспорта для участия в бонусной программе")
    @PostMapping("/upload-passport-photos")
    public ResponseEntity<String> uploadPassport(@RequestParam("photos") List<MultipartFile> photos, @CookieValue("refresh-jwt-cookie") String cookie) throws IOException {
        return ResponseEntity.ok(bonusService.uploadPhotosPassport(photos, cookie));
    }

    @Operation(summary = "Позволяет удалить типа штрих кода администратором по его id ")
    @DeleteMapping("delete-barcode-type/{id}")
    public ResponseEntity<Void> deleteBarcodeTypeById(@PathVariable Long id) {
        return bonusService.deleteBarcodeType(id);
    }

    @Operation(summary = "Удаление штрих кода по его id")
    @DeleteMapping("/delete-barcode/{id}")
    public ResponseEntity<Void> deleteBarcode(@PathVariable Long id) {
        return bonusService.deleteBarcode(id);
    }

    @Operation(summary = "Редактирование баланса пользователя по его e-mail")
    @PutMapping("/edit-balance")
    public ResponseEntity<String> updateBalance(@RequestParam String email, @RequestParam int amount) {
        return bonusService.updateBalance(email, amount);
    }

    @Operation(summary = "Редактирование типа штрих-кода по его id")
    @PutMapping("/update-barcode-type/{id}/type")
    public ResponseEntity<BarcodeType> updateBarcodeType(@PathVariable Long id, @RequestBody BarcodeTypeUpdateDto updateDto) {
        return bonusService.updateBarcodeType(id, updateDto);
    }

    @Operation(summary = "Редактирование штрих-кода по его id")
    @PutMapping("/update-barcode/{id}")
    public ResponseEntity<Barcode> updateBarcode(@PathVariable Long id, @RequestBody BarcodeUpdateDto updateDto) {
        return bonusService.updateBarcode(id, updateDto);
    }

    @Operation(summary = "Вывод всех штрих кодов")
    @GetMapping("/all-barcode")
    public ResponseEntity<List<BarcodeSummaryDto>> getAllBarcodesSummary() {
        return bonusService.getAllBarcodesSummary();
    }

    @Operation(summary = "Добавление нового типа штрих-кода")
    @PostMapping("/add-barcode-type")
    public ResponseEntity<BarcodeType> addBarcodeType(
            @RequestParam("points") int points,
            @RequestParam("type") String type,
            @RequestParam("subtype") String subtype) {
        return bonusService.addBarcodeType(points, type, subtype);
    }

    // Добавление нового штрих-кода
    @PostMapping("/add-barcode")
    @Operation(summary = "Добавление нового штрих-кода")
    public ResponseEntity<Barcode> addBarcode(@RequestParam Long code, @RequestParam Long barcodeTypeId) {
        return ResponseEntity.ok(barcodeService.addBarcode(code, barcodeTypeId));
    }

    @PostMapping("/updateStatus")
    @Operation(summary = "Обновление статуса запроса пользователя на получение баллов за бонусную программу")
    public ResponseEntity<String> updateBonusRequestStatus(
            @RequestParam("requestId") Long requestId,
            @RequestParam("status") RequestStatus status,
            @RequestParam(value = "rejectionMessage", required = false) String rejectionMessage) {
        return bonusService.updateBonusRequestStatus(requestId, status, rejectionMessage);
    }

    @PostMapping("/updateDocumentVerificationStatus")
    @Operation(summary = "Обновление статуса запроса пользователя на подтверждения паспортных данных")
    public ResponseEntity<String> updateDocumentVerificationStatus(
            @RequestParam("documentVerificationId") Long documentVerificationId,
            @RequestParam("status") RequestStatus status,
            @RequestParam(value = "rejectionMessage", required = false) String rejectionMessage) {
        return bonusService.updatePassportStatus(documentVerificationId, status, rejectionMessage);
    }
    @Operation(summary = "Получить все запросы пользователя по бонусной программе по email")
    @GetMapping("/get-user-bonus-requests")
    public ResponseEntity<List<Map<String, Object>>> getUserBonusRequests(@RequestParam String email) {
        return ResponseEntity.ok(bonusService.getUserBonusRequests(email));
    }
    @Operation(summary = "Получить по почтам все ожидающие/одобренные/отказанные запросы по бонусной программе: их статус, id, а также фотки котлов")
    @GetMapping("/get-all-bonus-requests-by-parameter")
    public ResponseEntity<List<Map<String, Object>>> getBonusRequests(RequestStatus requestStatus) {
        return bonusService.getBonusRequests(requestStatus);
    }
    @Operation(summary = "Получить бонусные запросы пользователя по email и статусу")
    @GetMapping("/get-user-bonus-requests-by-status")
    public ResponseEntity<List<Map<String, Object>>> getUserBonusRequestsByStatus(
            @RequestParam String email,
            @RequestParam RequestStatus status) {
        return ResponseEntity.ok(bonusService.getUserBonusRequestsByStatus(email, status));
    }
    @Operation(summary = "Получить по почтам все ожидающие/одобренные/отказанные запросы по паспортам: их статус, id, а также фотки паспортов")
    @GetMapping("/get-all-document-verifications-request-by-parameter")
    public ResponseEntity<List<Map<String, Object>>> getDocumentVerifications(RequestStatus requestStatus) {
        return bonusService.getDocumentVerifications(requestStatus);
    }
}
