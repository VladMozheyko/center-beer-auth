package fr.mossaab.security.backup.project.service.v1;


import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.backup.core.service.AbstractBackupVersionHandler;
import fr.mossaab.security.backup.project.dto.v1.*;
import fr.mossaab.security.backup.project.mappers.v1.*;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.Location;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BackupVersionHandlerV1 extends AbstractBackupVersionHandler {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final FileDataRepository fileDataRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    private final UserBackupMapperV1 userBackupMapper;
    private final LocationBackupMapperV1 locationBackupMapper;
    private final FileDataBackupMapperV1 fileDataBackupMapper;
    private final UserSocialAccountBackupMapperV1 userSocialAccountBackupMapper;


    public BackupVersionHandlerV1(ObjectMapper mapper,
                                  UserRepository userRepository,
                                  LocationRepository locationRepository,
                                  FileDataRepository fileDataRepository,
                                  UserSocialAccountRepository userSocialAccountRepository,
                                  UserBackupMapperV1 userBackupMapper,
                                  LocationBackupMapperV1 locationBackupMapper,
                                  FileDataBackupMapperV1 fileDataBackupMapper,
                                  UserSocialAccountBackupMapperV1 userSocialAccountBackupMapper) {
        super(mapper);
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.fileDataRepository = fileDataRepository;
        this.userSocialAccountRepository = userSocialAccountRepository;
        this.userBackupMapper = userBackupMapper;
        this.locationBackupMapper = locationBackupMapper;
        this.fileDataBackupMapper = fileDataBackupMapper;
        this.userSocialAccountBackupMapper = userSocialAccountBackupMapper;
    }

    @Override
    public int getSchemaVersion() {
        return 1; //меняется от версии к версии
    }

    @Override
        protected List<ExportTask<?, ?>> getExportTasks() {
            List<ExportTask<?, ?>> tasks = new ArrayList<>();

            tasks.add(new ExportTask<>(
                    "users",                  // entityName: как будет называться секция/файл с пользователями в бэкапе
                    userRepository,                     // repository: откуда брать пользователей для экспорта
                    userBackupMapper::toDto,            // mapper: как преобразовать User -> UserBackupDtoV1
                    user -> user.getId().toString()     // idExtractor: как получить строковый id пользователя для логов/ключей
            ));


            tasks.add(new ExportTask<>(
                    "fileData",
                    fileDataRepository,
                    fileDataBackupMapper::toDto,
                    fd -> fd.getId().toString()
            ));

            tasks.add(new ExportTask<>(
                    "locations",
                    locationRepository,
                    locationBackupMapper::toDto,
                    location -> location.getId().toString()
            ));

            tasks.add(new ExportTask<>(
                    "socialAccounts",
                    userSocialAccountRepository,
                    userSocialAccountBackupMapper::toDto,
                    socialAccount -> socialAccount.getId().toString()
            ));
            return tasks;
        }

    // ---------- IMPORT: только декларация того, что и как импортировать ----------

    @Override
    protected Class<?> getEnvelopeClass() {
        return BackupDataV1.class;
    }

    @Override
    protected ImportEnvelopeDescriptor buildImportDescriptor(Object envelopeObj) {
        // ==== ПЛАН МЕТОДА ====
        // 1) Приводим envelope и достаём списки DTO (user, fileData, location, socialAccount)
        // 2) Создаём мапы oldId -> сущность для каждой таблицы
        // 3) Описываем простые импорт-задачи (SimpleImportTask): "как превратить DTO в сущности и собрать мапы"
        // 4) Описываем задачи восстановления связей между уже сохранёнными сущностями (RelationRestoreTask)
        // ======================

        BackupDataV1 envelope = (BackupDataV1) envelopeObj;

        // 1) Достаём DTO, гарантируя, что списки не null
        List<UserBackupDtoV1> userDtos =
                envelope.getUsers() != null ? envelope.getUsers() : List.of();
        List<FileDataBackupDtoV1> fileDataDtos =
                envelope.getFileData() != null ? envelope.getFileData() : List.of();
        List<LocationBackupDtoV1> locationDtos =
                envelope.getLocations() != null ? envelope.getLocations() : List.of();
        List<UserSocialAccountBackupDtoV1> socialAccountDtos =
                envelope.getSocialAccounts() != null ? envelope.getSocialAccounts() : List.of();

        // 2) Мапы oldId -> новая сущность. Эти мапы заполняются во время SimpleImportTask.
        Map<Long, User> userByOldId = new HashMap<>();
        Map<Long, FileData> fileDataByOldId = new HashMap<>();
        Map<Long, Location> locationByOldId = new HashMap<>();
        Map<Long, UserSocialAccount> socialAccountByOldId = new HashMap<>();

        // СПИСОК простых импорт-задач
        List<SimpleImportTask<?, ?, ?>> simpleTasks = new ArrayList<>();


        // ----------- 3. СОЗДАНИЕ СУЩНОСТЕЙ БЕЗ СЯЗЕЙ -----------

        //3.1 Users
        simpleTasks.add(new SimpleImportTask<>(
                "User",
                userDtos,
                userBackupMapper::fromDto,          // DTO -> Entity
                userRepository,                     // куда сохраняем
                user -> user.setId(null),           // как обнуляем id
                UserBackupDtoV1::getId,             // как достать "старый id" из DTO
                userByOldId                         // сюда складываем соответствия oldId -> Entity
        ));

        // 3.2 FileData
        simpleTasks.add(new SimpleImportTask<>(
                "FileData",
                fileDataDtos,
                fileDataBackupMapper::fromDto,
                fileDataRepository,
                fd -> fd.setId(null),
                FileDataBackupDtoV1::getId,
                fileDataByOldId
        ));

        // 3.3 Locations
        simpleTasks.add(new SimpleImportTask<>(
                "Location",
                locationDtos,
                locationBackupMapper::fromDto,
                locationRepository,
                loc -> loc.setId(null),
                LocationBackupDtoV1::getId,
                locationByOldId
        ));

        // 3.4 UserSocialAccounts
        simpleTasks.add(new SimpleImportTask<>(
                "UserSocialAccount",
                socialAccountDtos,
                userSocialAccountBackupMapper::fromDto,
                userSocialAccountRepository,
                sa -> sa.setId(null),
                UserSocialAccountBackupDtoV1::getId,
                socialAccountByOldId
        ));


        // ----------- 4. ВОССТАНОВЛЕНИЕ СВЯЗЕЙ МЕЖДУ СУЩНОСТЯМИ -----------

        List<RelationRestoreTask> relationTasks = new ArrayList<>();

        relationTasks.add(
                relationChildToParent(
                        "FileData->User",
                        fileDataDtos,
                        FileDataBackupDtoV1::getId,               // старый id FileData
                        FileDataBackupDtoV1::getUserOriginalId,   // старый id User, к которому он относится
                        fileDataByOldId,                          // мапа oldFileDataId -> FileData
                        userByOldId,                              // мапа oldUserId -> User
                        FileData::setUser,                        // как выставить связь FileData -> User
                        fileDataRepository                        // в какой репозиторий сохраняем
                )
        );

        relationTasks.add(
                relationChildToParent(
                "UserSocialAccount->User",
                socialAccountDtos,
                UserSocialAccountBackupDtoV1::getId,
                UserSocialAccountBackupDtoV1::getUserOriginalId,
                socialAccountByOldId,
                userByOldId,
                UserSocialAccount::setUser,
                userSocialAccountRepository
                )
        );

        relationTasks.add(
                relationChildToParent(
                        "User->Location",
                        userDtos,
                        UserBackupDtoV1::getId,
                        UserBackupDtoV1::getLocationOriginalId,
                        userByOldId,
                        locationByOldId,
                        User::setLocation,
                        userRepository
                )
        );

        // Возвращаем единый объект, описывающий:
        //  - какие сущности импортировать;
        //  - какие связи после этого восстановить.
        return new ImportEnvelopeDescriptor(simpleTasks, relationTasks);
    }

}
