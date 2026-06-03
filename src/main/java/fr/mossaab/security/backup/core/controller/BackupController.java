package fr.mossaab.security.backup.core.controller;

import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.dto.response.BackupReportSummaryResponse;
import fr.mossaab.security.backup.core.service.BackupService;
import fr.mossaab.security.backup.core.service.impl.BackupHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backups")
@RequiredArgsConstructor
public class BackupController implements BackupControllerApi {

    private final BackupService backupService;
    private final BackupHistoryService backupHistoryService;
//    private final PreRestoreCleaner genericCleaner;

    @Override
    @PostMapping("/run")
    public ResponseEntity<BackupReport> run() {
        BackupReport report = backupService.exportSystemBackup();
        return ResponseEntity.ok(report);
    }

    @Override
    @PostMapping("/restore")
    public ResponseEntity<BackupReport> restore(
            @RequestParam String filename,
            @RequestParam BackupTier tier
    ) {
        BackupReport report = backupService.restoreSystemBackup(filename, tier);
        return ResponseEntity.ok(report);
    }

    @Override
    @GetMapping("/history")
    public ResponseEntity<List<BackupReportSummaryResponse>> listHistory(
            @RequestParam(name = "tier", required = false) BackupTier tier,
            @RequestParam(name = "status", required = false) BackupOperationStatus status
    ) {
        List<BackupReportSummaryResponse> list = backupHistoryService.listHistory(tier, status);
        return ResponseEntity.ok(list);
    }

    @Override
    @GetMapping("/report/{tier}")
    public ResponseEntity<BackupReport> getFullReport(
            @PathVariable("tier") BackupTier tier,
            @RequestParam("fileName") String fileName
    ) {
        BackupReport report = backupHistoryService.loadReport(fileName, tier);
        return ResponseEntity.ok(report);
    }

//    @PostMapping("/cleanup/all")
//    public void cleanAllEntity() {
//        genericCleaner.cleanAllEntities();
//    }
//
//    private final TestDataInitializer testDataInitializer;
//    @PostMapping("/init-test-data")
//    public void initTestData(@RequestParam boolean isCleanBD) {
//        testDataInitializer.initTestData(isCleanBD);
//    }
}