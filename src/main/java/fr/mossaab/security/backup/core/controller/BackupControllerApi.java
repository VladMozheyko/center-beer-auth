package fr.mossaab.security.backup.core.controller;


import fr.mossaab.security.backup.core.enums.BackupOperationStatus;
import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.report.BackupReport;
import fr.mossaab.security.backup.core.dto.response.BackupReportSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Backup",
        description = "Эндпоинты для ручного управления бэкапами: запуск, восстановление и просмотр истории."
)
@RequestMapping("/backups")
public interface BackupControllerApi {

    @Operation(
            summary = "Запустить создание резервной копии",
            description = """
                    POST /backups/run
                    
                    Запускает создание системного бэкапа (экспорт).
                    Возвращает подробный отчёт о выполненной операции.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Бэкап успешно создан или завершён с ошибкой (см. статус в теле)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BackupReport.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Успешный бэкап",
                                                    summary = "Пример успешного отчёта",
                                                    value = """
                                                            {
                                                              "archiveFileName": "backup-v1-20260613T134405Z-eb2d2095-2610-401f-a32b-4663096c268b.zip",
                                                              "operation": "EXPORT",
                                                              "schemaVersion": 1,
                                                              "startedAt": "2027-05-28T18:14:00.013548Z",
                                                              "finishedAt": "2027-05-28T18:14:01.013548Z",
                                                              "status": "SUCCESS",
                                                              "summary": {
                                                                "totalEntities": 1,
                                                                "processed": 1,
                                                                "skipped": 0,
                                                                "renamed": 0,
                                                                "errors": 0
                                                              },
                                                              "details": [
                                                                {
                                                                  "entityName": "users",
                                                                  "total": 1,
                                                                  "exported": 1,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "fileData",
                                                                  "total": 1,
                                                                  "exported": 0,
                                                                  "skipped": 1,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "locations",
                                                                  "total": 0,
                                                                  "exported": 0,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "socialAccounts",
                                                                  "total": 0,
                                                                  "exported": 0,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                }
                                                              ]
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Бекап с ошибками",
                                                    summary = "Пример отчета с ошибками",
                                                    value = """
                                                            {
                                                              "archiveFileName": "backup-v1-20260613T134405Z-eb2d2095-2610-401f-a32b-4663096c268b.zip",
                                                              "operation": "EXPORT",
                                                              "schemaVersion": 1,
                                                              "startedAt": "2026-06-13T13:44:05.454518800Z",
                                                              "finishedAt": "2026-06-13T13:44:05.604474900Z",
                                                              "status": "COMPLETED_WITH_WARNINGS",
                                                              "summary": {
                                                                "totalEntities": 2,
                                                                "processed": 1,
                                                                "skipped": 1,
                                                                "renamed": 0,
                                                                "errors": 0
                                                              },
                                                              "details": [
                                                                {
                                                                  "entityName": "users",
                                                                  "total": 1,
                                                                  "exported": 1,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "fileData",
                                                                  "total": 1,
                                                                  "exported": 0,
                                                                  "skipped": 1,
                                                                  "details": [
                                                                    {
                                                                      "status": "SKIPPED",
                                                                      "originalId": "1",
                                                                      "reasonCode": null,
                                                                      "reasonMessage": "Ошибка при экспорте сущности"
                                                                    }
                                                                  ]
                                                                },
                                                                {
                                                                  "entityName": "locations",
                                                                  "total": 0,
                                                                  "exported": 0,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "socialAccounts",
                                                                  "total": 0,
                                                                  "exported": 0,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                }
                                                              ]
                                                            }
                                                    """)
                                    }
                            )
                    )
            }
    )
    @PostMapping("/run")
    ResponseEntity<BackupReport> run();

    @Operation(
            summary = "Восстановить систему из резервной копии",
            description = """
                    POST /backups/restore
                    
                    Выполняет операцию восстановления (import) из указанного ZIP‑файла бэкапа.
                    Возвращает подробный отчёт о результате восстановления.
                    """,
            parameters = {
                    @Parameter(
                            name = "filename",
                            description = "Имя файла бэкапа (как хранится в хранилище, включая расширение .zip)",
                            required = true,
                            example = "backup-v1_0_0-20260526T110401Z-00001.zip"
                    ),
                    @Parameter(
                            name = "tier",
                            description = "Уровень хранения, где искать бэкап",
                            required = true,
                            example = "DAILY"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Операция восстановления завершена (см. статус в теле)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BackupReport.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Успешное восстановление",
                                                    summary = "Пример успешного восстановления",
                                                    value = """
                                                            {
                                                                "fileName": "backup-v1-20260602T104159-4ef04126-792f-4b99-8c81-b5c872562773.zip",
                                                                "operation": "RESTORE",
                                                                "schemaVersion": 1,
                                                                "startedAt": "2026-06-02T07:47:29.400308Z",
                                                                "finishedAt": "2026-06-02T07:47:29.537330600Z",
                                                                "status": "SUCCESS",
                                                                "summary": {
                                                                "totalEntities": 13,
                                                                "processed": 13,
                                                                "skipped": 0,
                                                                "errors": 0
                                                            },
                                                            "details": [
                                                                {
                                                                  "entityName": "User",
                                                                  "total": 4,
                                                                  "exported": 4,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "FileData",
                                                                  "total": 3,
                                                                  "exported": 3,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "Location",
                                                                  "total": 4,
                                                                  "exported": 4,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "UserSocialAccount",
                                                                  "total": 2,
                                                                  "exported": 2,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                }
                                                              ]
                                                            }
                                                            """
                                            ), @ExampleObject(
                                                    name = "Восстановление с ошибками",
                                            summary = "Пример восстановления с ошибками",
                                            value = """
                                                    {
                                                      "fileName": "backup-v1-20260601T160100-12c34b25-b079-4807-9392-eb34a34db5ec.zip",
                                                      "operation": "RESTORE",
                                                      "schemaVersion": 1,
                                                      "startedAt": "2026-06-02T07:51:09.041495500Z",
                                                      "finishedAt": "2026-06-02T07:51:09.214495400Z",
                                                      "status": "COMPLETED_WITH_WARNINGS",
                                                      "summary": {
                                                        "totalEntities": 17,
                                                        "processed": 13,
                                                        "skipped": 4,
                                                        "errors": 4
                                                      },
                                                      "details": [
                                                        {
                                                          "entityName": "User",
                                                          "total": 5,
                                                          "exported": 4,
                                                          "skipped": 1,
                                                          "details": [
                                                            {
                                                              "status": "ERROR",
                                                              "originalId": "8",
                                                              "reasonCode": null,
                                                              "reasonMessage": "Ошибка при создании сущности"
                                                            }
                                                          ]
                                                        },
                                                        {
                                                          "entityName": "FileData",
                                                          "total": 4,
                                                          "exported": 3,
                                                          "skipped": 1,
                                                          "details": [
                                                            {
                                                              "status": "ERROR",
                                                              "originalId": "6",
                                                              "reasonCode": null,
                                                              "reasonMessage": "Ошибка при создании сущности"
                                                            }
                                                          ]
                                                        },
                                                        {
                                                          "entityName": "Location",
                                                          "total": 5,
                                                          "exported": 4,
                                                          "skipped": 1,
                                                          "details": [
                                                            {
                                                              "status": "ERROR",
                                                              "originalId": "7",
                                                              "reasonCode": null,
                                                              "reasonMessage": "Ошибка при создании сущности"
                                                            }
                                                          ]
                                                        },
                                                        {
                                                          "entityName": "UserSocialAccount",
                                                          "total": 3,
                                                          "exported": 2,
                                                          "skipped": 1,
                                                          "details": [
                                                            {
                                                              "status": "ERROR",
                                                              "originalId": "5",
                                                              "reasonCode": null,
                                                              "reasonMessage": "Ошибка при создании сущности"
                                                            }
                                                          ]
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ), @ExampleObject(
                                            name = "Ошибка при восстановлении",
                                            summary = "Пример отчета при ошибке восстановления",
                                            value = """
                                                    {
                                                      "fileName": "backup-v1_0_0-202рап60526T110401Z-00001.zip",
                                                      "operation": "RESTORE",
                                                      "schemaVersion": 0,
                                                      "startedAt": "2026-06-02T08:12:45.062568500Z",
                                                      "finishedAt": "2026-06-02T08:12:45.081582800Z",
                                                      "status": "FAILED",
                                                      "summary": {
                                                        "totalEntities": 0,
                                                        "processed": 0,
                                                        "skipped": 0,
                                                        "errors": 1
                                                      },
                                                      "details": [
                                                        {
                                                          "entityName": "backup-v1_0_0-202рап60526T110401Z-00001.zip",
                                                          "total": 0,
                                                          "exported": 0,
                                                          "skipped": 0,
                                                          "details": [
                                                            {
                                                              "status": null,
                                                              "originalId": null,
                                                              "reasonCode": null,
                                                              "reasonMessage": "Backup file not found: C:\\\\var\\\\path\\\\to\\\\backups\\\\DAILY\\\\backup-v1_0_0-202рап60526T110401Z-00001.zip"
                                                            }
                                                          ]
                                                        }
                                                      ]
                                                    }
                                                    """

                                    )
                                    }
                            )
                    )
            }
    )
    @PostMapping("/restore")
    ResponseEntity<BackupReport> restore(
            @RequestParam String filename,
            @RequestParam BackupTier tier
    );

    @Operation(
            summary = "Получить историю бэкапов",
            description = """
                    GET /backups/history
                    
                    Возвращает список кратких отчётов по всем найденным бэкапам.
                    Фильтры по уровню хранения и статусу операции являются необязательными.
                    
                    Примеры запросов:
                    - GET /backups/history
                    - GET /backups/history?tier=DAILY
                    - GET /backups/history?status=SUCCESS
                    - GET /backups/history?tier=WEEKLY&status=FAILED
                    """,
            parameters = {
                    @Parameter(
                            name = "tier",
                            description = "Уровень хранения. Если не указан, берутся все уровни.",
                            example = "DAILY"
                    ),
                    @Parameter(
                            name = "status",
                            description = "Статус операции. Если не указан, берутся все статусы.",
                            example = "SUCCESS"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список найденных бэкапов",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = BackupReportSummaryResponse.class)
                                    ),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Пример списка истории",
                                                    summary = "Несколько успешных и один неуспешный бэкап",
                                                    value = """
                                                            [
                                                              {
                                                                "fileName": "backup-v1_0_0-20260526T110401Z-00001.zip",
                                                                "tier": "DAILY",
                                                                "operation": "EXPORT",
                                                                "status": "SUCCESS",
                                                                "schemaVersion": 1,
                                                                "startedAt": "2026-05-26T11:04:01Z",
                                                                "finishedAt": "2026-05-26T11:04:10Z",
                                                                "totalEntities": 152,
                                                                "processed": 152,
                                                                "errors": 0
                                                              },
                                                              {
                                                                "fileName": "backup-v1_0_0-20260525T110401Z-00001.zip",
                                                                "tier": "DAILY",
                                                                "operation": "EXPORT",
                                                                "status": "FAILED",
                                                                "schemaVersion": 1,
                                                                "startedAt": "2026-05-25T11:04:01Z",
                                                                "finishedAt": "2026-05-25T11:04:05Z",
                                                                "totalEntities": 152,
                                                                "processed": 50,
                                                                "errors": 1
                                                              }
                                                            ]
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    @GetMapping("/history")
    ResponseEntity<List<BackupReportSummaryResponse>> listHistory(
            @RequestParam(name = "tier", required = false) BackupTier tier,
            @RequestParam(name = "status", required = false) BackupOperationStatus status
    );

    @Operation(
            summary = "Получить полный отчёт по конкретному бэкапу",
            description = """
                    GET /backups/report/{tier}
                    
                    Возвращает полный отчёт на каждую сущность в бекапе (из файла report.json) для указанного ZIP‑файла бэкапа.
                    Важно!!! часть пути DAILY/WEEKLY/.. большими буквами.
                    Пример:
                    GET /backups/report/DAILY?fileName=backup-v1_0_0-20260526T110401Z-00001.zip
                    """,
            parameters = {
                    @Parameter(
                            name = "tier",
                            description = "Уровень хранения, в котором расположен бэкап",
                            required = true,
                            example = "DAILY"
                    ),
                    @Parameter(
                            name = "fileName",
                            description = "Имя ZIP‑файла бэкапа",
                            required = true,
                            example = "backup-v1_0_0-20260526T110401Z-00001.zip"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Полный отчёт по указанному бэкапу на каждый тип сущности",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BackupReport.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Пример полного отчёта",
                                                    summary = "Полный отчет по конкретному бекапу",
                                                    value = """
                                                            {
                                                            {
                                                              "fileName": "backup-v1-20260601T160100-12c34b25-b079-4807-9392-eb34a34db5ec.zip",
                                                              "operation": "EXPORT",
                                                              "schemaVersion": 1,
                                                              "startedAt": "2026-06-01T13:01:00.012911800Z",
                                                              "finishedAt": "2026-06-01T13:01:00.134904900Z",
                                                              "status": "SUCCESS",
                                                              "summary": {
                                                                "totalEntities": 17,
                                                                "processed": 17,
                                                                "skipped": 0,
                                                                "errors": 0
                                                              },
                                                              "details": [
                                                                {
                                                                  "entityName": "users",
                                                                  "total": 5,
                                                                  "exported": 5,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "fileData",
                                                                  "total": 4,
                                                                  "exported": 4,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "locations",
                                                                  "total": 5,
                                                                  "exported": 5,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                },
                                                                {
                                                                  "entityName": "socialAccounts",
                                                                  "total": 3,
                                                                  "exported": 3,
                                                                  "skipped": 0,
                                                                  "details": []
                                                                }
                                                              ]
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    @GetMapping("/report/{tier}")
    ResponseEntity<BackupReport> getFullReport(
            @PathVariable("tier") BackupTier tier,
            @RequestParam("fileName") String fileName
    );
}
