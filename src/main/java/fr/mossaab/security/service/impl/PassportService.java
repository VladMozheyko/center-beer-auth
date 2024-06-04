package fr.mossaab.security.service.impl;

import fr.mossaab.security.config.PathConfig;
import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.PassportCategory;
import fr.mossaab.security.entities.PassportFileData;
import fr.mossaab.security.entities.PassportTitle;
import fr.mossaab.security.repository.ErrorRepository;
import fr.mossaab.security.repository.PassportCategoryRepository;
import fr.mossaab.security.repository.PassportFileDataRepository;
import fr.mossaab.security.repository.PassportTitleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class PassportService {
    @Autowired
    private PassportFileDataRepository passportFileDataRepository;

    @Autowired
    private PassportTitleRepository passportTitleRepository;

    @Autowired
    private PassportCategoryRepository passportCategoryRepository;
    @Autowired
    private ErrorRepository errorRepository;

    public PassportFileData uploadImage(PassportTitle passportTitle, String number) throws IOException {
        String str1 = "/var/www/vuary/Инструкции и коды ошибок/";
        String str2 = passportTitle.getCategory().getTitle();
        String str3 = passportTitle.getTitle();
        String filePath = str1 +str2+"/"+ str3 + "/" + str3 + number + ".png";

        PassportFileData fileDataPresentation = passportFileDataRepository.save(PassportFileData.builder()
                .name(str3 + number + ".png")
                .type("image/jpeg")
                .filePath(filePath).passportTitle(passportTitle).build());
        passportFileDataRepository.save(fileDataPresentation);
        return fileDataPresentation;
    }

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<PassportFileData> fileData = passportFileDataRepository.findByName(fileName);
        String filePath=fileData.get().getFilePath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }

    @Transactional
    public void createAndSavePassportData() throws IOException {
        if (passportCategoryRepository.count() == 0 && passportTitleRepository.count() == 0 && passportFileDataRepository.count() == 0 && errorRepository.count() == 0) {
            // Создание категорий
            PassportCategory category1 = new PassportCategory();
            category1.setTitle("Salmi");
            PassportCategory category2 = new PassportCategory();
            category2.setTitle("Ainova");
            PassportCategory category3 = new PassportCategory();
            category3.setTitle("Toivo");
            PassportCategory category4 = new PassportCategory();
            category4.setTitle("Suari");
            passportCategoryRepository.saveAll(List.of(category1, category2, category3, category4));

            // Создание заголовков паспортов
            PassportTitle title1 = new PassportTitle();
            title1.setTitle("Паспорт водонагреватель VHRM");
            title1.setCategory(category1);

            PassportTitle title2 = new PassportTitle();
            title2.setTitle("Паспорт водонагреватель HVFM");
            title2.setCategory(category1);

            PassportTitle title3 = new PassportTitle();
            title3.setTitle("Passport_VFE_A5_new");
            title3.setCategory(category1);

            PassportTitle title4 = new PassportTitle();
            title4.setTitle("Passport_KM_A5_new");
            title4.setCategory(category1);

            PassportTitle title5 = new PassportTitle();
            title5.setTitle("Passport_FRM_A5_new");
            title5.setCategory(category1);

            PassportTitle title6 = new PassportTitle();
            title6.setTitle("Passport_FRM_A5_3mm_new");
            title6.setCategory(category1);

            PassportTitle title7 = new PassportTitle();
            title7.setTitle("Паспорт электрический котел QM");
            title7.setCategory(category2);

            PassportTitle title8 = new PassportTitle();
            title8.setTitle("Паспорт электрический котел LT-D");
            title8.setCategory(category2);

            PassportTitle title9 = new PassportTitle();
            title9.setTitle("Паспорт Т30-40DK");
            title9.setCategory(category3);

            PassportTitle title10 = new PassportTitle();
            title10.setTitle("Паспорт T10-24 OK");
            title10.setCategory(category3);

            PassportTitle title11 = new PassportTitle();
            title11.setTitle("10-24DK");
            title11.setCategory(category3);

            PassportTitle title12 = new PassportTitle();
            title12.setTitle("Паспорт колонка Suari ST_FT");
            title12.setCategory(category4);

            PassportTitle title13 = new PassportTitle();
            title13.setTitle("Паспорт колонка S10-12, S10-12EM");
            title13.setCategory(category4);

            passportTitleRepository.saveAll(List.of(title1, title2, title3, title4, title5, title6, title7, title8, title9, title10, title11, title12, title13));

            for (int i = 1; i <= 12; i++) {
                uploadImage(title1, "-" + i);
            }
            for (int i = 1; i <= 12; i++) {
                uploadImage(title2, "-" + i);
            }
            for (int i = 1; i <= 12; i++) {
                uploadImage(title3, "-" + i);
            }
            for (int i = 1; i <= 12; i++) {
                uploadImage(title4, "-" + i);
            }
            for (int i = 1; i <= 12; i++) {
                uploadImage(title5, "-" + i);
            }
            for (int i = 1; i <= 12; i++) {
                uploadImage(title6, "-" + i);
            }
            for (int i = 1; i <= 40; i++) {
                uploadImage(title7, "-" + i);
            }
            for (int i = 1; i <= 32; i++) {
                uploadImage(title8, "-" + i);
            }
            for (int i = 1; i <= 53; i++) {
                uploadImage(title9, "-" + i);
            }
            for (int i = 1; i <= 51; i++) {
                uploadImage(title10, "-" + i);
            }
            for (int i = 1; i <= 55; i++) {
                uploadImage(title11, "-" + i);
            }
            for (int i = 1; i <= 26; i++) {
                uploadImage(title12, "-" + i);
            }
            for (int i = 1; i <= 29; i++) {
                uploadImage(title13, "-" + i);
            }

            /*Error error1 = new Error();
            error1.setCode("E0");
            error1.setDescription("Автоматическое восстановление после\n" +
                    "повышения температуры теплоносителя\n" +
                    "на 1°С");
            error1.setCause("Ошибка режима «АНТИЗАМОРОЗКА»");
            error1.setCategory(category2);
            errorRepository.save(error1);

            Error error2 = new Error();
            error2.setCode("E1");
            error2.setDescription("Нажмите и удерживайте кнопку «Reset»\n" +
                    "для удаления ошибки");
            error2.setCause("Ошибка в электрической цепи котла");
            error2.setCategory(category2);
            errorRepository.save(error2);

            Error error3 = new Error();
            error3.setCode("E3");
            error3.setDescription("Нажмите и удерживайте кнопку «Reset»\n" +
                    "для удаления ошибки");
            error3.setCause("Обрыв электрической цепи аварийного\n" +
                    "датчика температуры");
            error3.setCategory(category2);
            errorRepository.save(error3);

            Error error4 = new Error();
            error4.setCode("E4");
            error4.setDescription("Нажмите и удерживайте кнопку «Reset»\n" +
                    "для удаления ошибки");
            error4.setCause("Перегрев датчика температуры линии\n" +
                    "подачи");
            error4.setCategory(category2);
            errorRepository.save(error4);

            Error error5 = new Error();
            error5.setCode("E7");
            error5.setDescription("Автоматическое восстановление");
            error5.setCause("Ошибка датчика температуры\n" +
                    "теплоносителя");
            error5.setCategory(category2);
            errorRepository.save(error5);

            Error error6 = new Error();
            error6.setCode("EА");
            error6.setDescription("Автоматическое восстановление");
            error6.setCause("Ошибка датчика реле протока.\n" +
                    "(фактический проток есть, но нет сигнала\n" +
                    "от датчика)");
            error6.setCategory(category2);
            errorRepository.save(error6);

            Error error7 = new Error();
            error7.setCode("EР");
            error7.setDescription("Автоматическое восстановление");
            error7.setCause("Ошибка датчика реле протока\n" +
                    "(отсутствие циркуляция теплоносителя)");
            error7.setCategory(category2);
            errorRepository.save(error7);

            Error error8 = new Error();
            error8.setCode("Е1");
            error8.setDescription("Неполадки, связанные с\n" +
                    "неудачным розжигом. Котел не\n" +
                    "работает");
            error8.setCause("Нет подачи газа или не открыт газовый кран;\n" +
                    "Неисправны электроды розжига;\n" +
                    "Неисправность газового\n" +
                    "клапана;\n" +
                    "Пониженное давление\n" +
                    "газа;\n" +
                    "Неисправность датчика\n" +
                    "контроля пламени;\n" +
                    "Выход из строя платы\n" +
                    "управления.");
            error8.setCategory(category3);
            errorRepository.save(error8);

            Error error9 = new Error();
            error9.setCode("Е2");
            error9.setDescription("Перегрев теплоносителя (≥95°C)");
            error9.setCause("Неисправность датчика\n" +
                    "защиты от перегрева;\n" +
                    "Обрыв соединительного\n" +
                    "кабеля датчика перегрева;\n" +
                    "Неисправность системы\n" +
                    "защиты от перегрева.");
            error9.setCategory(category3);
            errorRepository.save(error9);

            Error error10 = new Error();
            error10.setCode("Е3");
            error10.setDescription("Неисправность системы\n" +
                    "дымоудаления");
            error10.setCause("Неисправность\n" +
                    "вентилятора;\n" +
                    "Отказ прессостата (только\n" +
                    "модели без инверторной\n" +
                    "турбины);\n" +
                    "Засорение трубы\n" +
                    "дымоудаления.");
            error10.setCategory(category3);
            errorRepository.save(error10);

            Error error11 = new Error();
            error11.setCode("Е4");
            error11.setDescription("Недостаточное давление\n" +
                    "теплоносителя в контуре\n" +
                    "отопления.");
            error11.setCause("Воздушная пробка в\n" +
                    "системе отопления;\n" +
                    "Неисправен датчик\n" +
                    "давления теплоносителя в\n" +
                    "контуре отопления;");
            error11.setCategory(category3);
            errorRepository.save(error11);

            Error error12 = new Error();
            error12.setCode("Е5");
            error12.setDescription("Неисправность в напряжении\n" +
                    "электромагнитного клапана");
            error12.setCause("Плата управления выдает\n" +
                    "неправильное напряжение\n" +
                    "на электромагнитный клапан газового клапана.");
            error12.setCategory(category3);
            errorRepository.save(error12);

            Error error13 = new Error();
            error13.setCode("Е6");
            error13.setDescription("Повреждение датчика\n" +
                    "температуры контура ГВС");
            error13.setCause("Повреждение датчика\n" +
                    "температуры (обрыв цепи,\n" +
                    "короткое замыкание);\n" +
                    "Обрыв соединительного\n" +
                    "кабеля датчика температуры.");
            error13.setCategory(category3);
            errorRepository.save(error13);

            Error error14 = new Error();
            error14.setCode("Е7");
            error14.setDescription("Повреждение датчика\n" +
                    "температуры отопления");
            error14.setCause("Повреждение датчика\n" +
                    "температуры (обрыв цепи,\n" +
                    "короткое замыкание);\n" +
                    "Обрыв соединительного\n" +
                    "кабеля датчика температуры.");
            error14.setCategory(category3);
            errorRepository.save(error14);

            Error error15 = new Error();
            error15.setCode("Е8");
            error15.setDescription("Перегрев (≥ 90°C)");
            error15.setCause("Неисправность датчика\n" +
                    "температуры;\n" +
                    "Неисправность системы\n" +
                    "защиты от перегрева.");
            error15.setCategory(category3);
            errorRepository.save(error15);

            Error error16 = new Error();
            error16.setCode("Е9");
            error16.setDescription("Обледенение (≤1°C)");
            error16.setCause("Система отопления может\n" +
                    "быть заморожена");
            error16.setCategory(category3);
            errorRepository.save(error16);+*/
        }
    }
}
