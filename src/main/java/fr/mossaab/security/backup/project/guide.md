### Информация для быстрого запуска бекапа

1. Добавить в pom.xml зависимости

```xml
    <!-- MAPSTRUCT -->
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.4.2.Final</version> <!-- Версия может быть другой -->
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.4.2.Final</version> <!-- Версия должна совпадать с версией mapstruct -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

2. Внести настройки в
- application.properties:

```properties
app.version=1.0.0
```
- application-{profile}.properties
```properties
# ==================================================
# BACKUP
# ==================================================
backup.current-schema-version=1
backup.app-version=${app.version}
backup.formatted-json=true
backup.storage-type=FILE_SYSTEM
backup.file-system.base-path=/var/path/to/backups

# ===== retention policy =====
# how many days to store daily backups
backup.retention.daily-days=7
# how many weeks to keep weekly backups
backup.retention.weekly-weeks=12
# how many months to keep monthly-monthly backups
backup.retention.monthly-months=24
# How many years to keep semi-annual (SEMI_ANNUAL) backups.
backup.retention.semi-annual-years=5
# how many years to keep annual-annual backups
backup.retention.annual-years=10

# +++++++++++ backup schedule +++++++++++
# daily backup creation 0 0 1 * * * = every day at 01 00 00
backup.schedule.daily-backup-cron=0 0 1 * * *

# +++++++++++ promotion +++++++++++
# Promotion DAILY -> WEEKLY:
# 0 0 2 * * * = every day at 02:00:00.
backup.schedule.promote-daily-to-weekly-cron=0 0 2 * * *
# Promotion WEEKLY -> MONTHLY:
# 0 5 2 * * * = every day at 02:05:00.
backup.schedule.promote-weekly-to-monthly-cron=0 5 2 * * *
# Promotion MONTHLY -> SEMI_ANNUAL:
# 0 10 2 * * * = every day at 02:10:00.
backup.schedule.promote-monthly-to-semi-annual-cron=0 10 2 * * *
# Promotion SEMI_ANNUAL -> ANNUAL:
# 0 15 2 * * * = every day at 02:15:00.
backup.schedule.promote-semi-annual-to-annual-cron=0 15 2 * * *

# ++++++++++++ applying a retention policy (deleting old backups): ++++++++++++
# 0 30 3 * * * = every day at 03 30 00
backup.schedule.retention-cron=0 30 3 * * *
```
3. перенести пакет beckup в src/main/java/ваш проект/

4. поправить импорты (можно массово для всего проекта заменить импорты выделив неправильный пакет приложения Ctrl+Shift+R и заменить на необходимый)

5. переписать файлы для project v1

BackupDataV1.java - класс который будет содержать сущности для бекапа (имя списка сущностей должно соответствовать имени которое потом будете использовать в new ExportTask<> {entityName})

{Имя Сущности}BackupDtoV1 UpdateConfigBackupDtoV1 - тут переписываем необходимые поля для восстановления и бекапа.
Если у сущности есть связи то:
- Сущность владелец связи (@Column(name="entity_id")), то в дто ее делаем поле для id сущности на которую будет ссылка, например entityOriginalId (в маппере нужно будет указать как мапить такое поле)
- Сущность не владелец (@Column(mapped="entity")), в таком случае связь будем делать у второй сущности на эту сущность

{Имя Сущности}MapperV1 UpdateConfigMapperV1 - тут делаем простой маппер управляемый спрингом
```java
@Mapper(componentModel = "spring")
public interface EntityMapperV1 extends BackupMapper<Entity, EntityBackupDtoV1> {
}
```

если имеются поля не соответствующие бд, необходимо указать это мапперу
```java
@Mapper(componentModel = "spring")
public interface EntityBackupMapperV1 extends BackupMapper<Entity, EntityBackupDtoV1> {

    @Mapping(source = "simpleEntity.id", target = "simpleEntityOriginalId")
    EntityBackupDtoV1 toDto(Entity entity);
}
```

6. переписать класс BackupVersionHandlerV1 extends AbstractBackupVersionHandler

- зависимости указать свои и создать конструктор к ним, указать ObjectMapper для супер класса
- переопределить getSchemaVersion() он должен возвращать версию бекапа под которую пишется код (1,2,3,4...)
- в getExportTasks()  создать задачи на экспорт данных, тоесть описать в задаче сущность которую необходимо бекапить. И так для каждой потом собрать в коллекцию и вернуть
- getEnvelopeClass() должен возвратиь класс в котором будут хранится все необходимые сущности для текущей версии BackupDataV1, V2...
- buildImportDescriptor(Object envelopeObj)  тут необходимо:
    - достаем данные дто из envelop
    -  cоздаем задачи на восстановление сущностей без связей SimpleImportTask<?, ?, ?>
    - показываем как восстанавливать связи и кладем их в RelationRestoreTask, используя помошник relationChildToParent()
    - в конце собираем список всех тасков в ImportEnvelopeDescriptor(simpleTasks, relationTasks) и возвращаем

### Может понадобится

если возникают ошибки при десереллизации то можно добавить компонент маппер и использовать его
```java
@Component
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
```


