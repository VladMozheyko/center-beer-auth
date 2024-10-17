package fr.mossaab.security.service.init;

import fr.mossaab.security.entities.Error;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.StorageService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class PassportService {
    @Autowired
    private PassportTitleRepository passportTitleRepository;
    @Autowired
    private PassportCategoryRepository passportCategoryRepository;
    @Autowired
    private ErrorRepository errorRepository;
    @Autowired
    private SeriesTitleRepository seriesTitleRepository;
    @Autowired
    private StorageService storageService;

    @Transactional
    public void createAndSavePassportData() throws IOException {
        if (passportCategoryRepository.count() == 0 &&
                passportTitleRepository.count() == 0 &&
                errorRepository.count() == 0 &&
                seriesTitleRepository.count() == 0
        ) {
            // Создание категорий
            PassportCategory category1 = new PassportCategory();
            category1.setTitle("Salmi");
            category1.setRuTitle("Электрические накопительные водонагреватели");
            PassportCategory category2 = new PassportCategory();
            category2.setTitle("Ainova");
            category2.setRuTitle("Электрические котлы");
            PassportCategory category3 = new PassportCategory();
            category3.setTitle("Toivo");
            category3.setRuTitle("Газовые котлы");
            PassportCategory category4 = new PassportCategory();
            category4.setTitle("Suari");
            category4.setRuTitle("Газовые колонки");
            passportCategoryRepository.saveAll(List.of(category1, category2, category3, category4));

            // Создание заголовков паспортов
            PassportTitle title1 = new PassportTitle();
            title1.setTitle("Passport_VHRM");
            title1.setRuTitle("Паспорт водонагреватель VHRM");
            title1.setCategory(category1);

            PassportTitle title2 = new PassportTitle();
            title2.setTitle("Passport_HVFM");
            title2.setRuTitle("Паспорт водонагреватель HVFM");
            title2.setCategory(category1);

            PassportTitle title3 = new PassportTitle();
            title3.setTitle("Passport_VFE_A5");
            title3.setRuTitle("Паспорт водонагреватель VFE_A5");
            title3.setCategory(category1);

            PassportTitle title4 = new PassportTitle();
            title4.setTitle("Passport_KM_A5");
            title4.setRuTitle("Паспорт водонагреватель KM_A5");
            title4.setCategory(category1);

            PassportTitle title5 = new PassportTitle();
            title5.setTitle("Passport_FRM_A5");
            title5.setRuTitle("Паспорт водонагреватель FRM_A5");
            title5.setCategory(category1);

            PassportTitle title7 = new PassportTitle();
            title7.setTitle("Passport_QM");
            title7.setRuTitle("Паспорт электрический котел_QM");
            title7.setCategory(category2);

            PassportTitle title8 = new PassportTitle();
            title8.setTitle("Passport_LT_D");
            title8.setRuTitle("Паспорт электрический котел_LT_D");
            title8.setCategory(category2);

            PassportTitle title9 = new PassportTitle();
            title9.setTitle("Passport_Т30_40DK");
            title9.setRuTitle("Паспорт Т30-40DK");
            title9.setCategory(category3);

            PassportTitle title10 = new PassportTitle();
            title10.setTitle("Passport_T10_24OK");
            title10.setRuTitle("Паспорт T10-24 OK");
            title10.setCategory(category3);

            PassportTitle title11 = new PassportTitle();
            title11.setTitle("Passport_T10_24DK");
            title11.setRuTitle("Паспорт Т10-24DK");
            title11.setCategory(category3);

            PassportTitle title12 = new PassportTitle();
            title12.setTitle("Passport_ST_FT");
            title12.setRuTitle("Паспорт колонка ST_FT");
            title12.setCategory(category4);

            PassportTitle title13 = new PassportTitle();
            title13.setTitle("Passport_S10_12_S10_12EM");
            title13.setRuTitle("Паспорт колонка S10-12, S10-12EM");
            title13.setCategory(category4);

            passportTitleRepository.saveAll(List.of(title1, title2, title3, title4, title5, title7, title8, title9, title10, title11, title12, title13));

            List<PassportTitle> titles = Arrays.asList(
                    title1, title2, title3, title4, title5,
                    title7, title8, title9, title10, title11,
                    title12, title13
            );

            for (PassportTitle title : titles) {
                storageService.uploadImageToFileSystem(null, title.getTitle(), title);
            }
            //////////////////////////////
            //////////T10-T24OK, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK и T30 O ÷ T50 O////////////
            //////////////////////////////
            SeriesTitle seriesTitle1 = new SeriesTitle();
            seriesTitle1.setPassportCategory(category3);
            seriesTitle1 = seriesTitleRepository.save(seriesTitle1);

            SeriesTitle seriesTitle2 = new SeriesTitle();
            seriesTitle2.setPassportCategory(category3);
            seriesTitle2 = seriesTitleRepository.save(seriesTitle2);

            SeriesTitle seriesTitle3 = new SeriesTitle();
            seriesTitle3.setPassportCategory(category4);
            seriesTitle3 = seriesTitleRepository.save(seriesTitle3);

            SeriesTitle seriesTitle4 = new SeriesTitle();
            seriesTitle4.setPassportCategory(category4);
            seriesTitle4 = seriesTitleRepository.save(seriesTitle4);

            SeriesTitle seriesTitle5 = new SeriesTitle();
            seriesTitle5.setPassportCategory(category2);
            seriesTitle5 = seriesTitleRepository.save(seriesTitle5);

            SeriesTitle seriesTitle6 = new SeriesTitle();
            seriesTitle6.setPassportCategory(category2);
            seriesTitle6 = seriesTitleRepository.save(seriesTitle6);


            seriesTitle1.setTitle("T10-T24OK, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK и T30 O ÷ T50 O");

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
            error8.setSeriesTitle(seriesTitle1);
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
            error9.setSeriesTitle(seriesTitle1);
            //error9.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
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
            error10.setSeriesTitle(seriesTitle1);
            //error10.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
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
            error11.setSeriesTitle(seriesTitle1);
            //error11.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
            errorRepository.save(error11);

            Error error12 = new Error();
            error12.setCode("Е5");
            error12.setDescription("Неисправность в напряжении\n" +
                    "электромагнитного клапана");
            error12.setCause("Плата управления выдает\n" +
                    "неправильное напряжение\n" +
                    "на электромагнитный клапан газового клапана.");
            error12.setSeriesTitle(seriesTitle1);
            //error12.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
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
            error13.setSeriesTitle(seriesTitle1);
            //error13.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
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
            error14.setSeriesTitle(seriesTitle1);
            //error14.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
            errorRepository.save(error14);

            Error error15 = new Error();
            error15.setCode("Е8");
            error15.setDescription("Перегрев (≥ 90°C)");
            error15.setCause("Неисправность датчика\n" +
                    "температуры;\n" +
                    "Неисправность системы\n" +
                    "защиты от перегрева.");
            error15.setSeriesTitle(seriesTitle1);
            //error15.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
            errorRepository.save(error15);

            Error error16 = new Error();
            error16.setCode("Е9");
            error16.setDescription("Обледенение (≤1°C)");
            error16.setCause("Система отопления может\n" +
                    "быть заморожена");
            error16.setSeriesTitle(seriesTitle1);
            //error16.setSeries("T100K - T240K, T10-T24DK, T10 TKP(TKL) ÷ T24 TKP(TKL), T30 OK ÷ T50 OK,T30 O ÷ T50 O");
            errorRepository.save(error16);


            //////////////////////////////
            //////////T32-36DK////////////
            //////////////////////////////

            seriesTitle2.setTitle("T32-36DK");

            Error error29 = new Error();
            error29.setCode("Е1");
            error29.setDescription("Неполадки, связанные с\n" +
                    "неудачным розжигом. Котел не\n" +
                    "работает");
            error29.setCause("Нет подачи газа или не открыт газовый кран;\n" +
                    "Неисправны электроды розжига;\n" +
                    "Неисправность газового\n" +
                    "клапана;\n" +
                    "Пониженное давление\n" +
                    "газа;\n" +
                    "Неисправность датчика\n" +
                    "контроля пламени;\n" +
                    "Выход из строя платы\n" +
                    "управления.");
            error29.setSeriesTitle(seriesTitle2);
            //error29.setSeries("T32-36DK");
            errorRepository.save(error29);

            Error error30 = new Error();
            error30.setCode("Е2");
            error30.setDescription("Перегрев теплоносителя (≥95°C)");
            error30.setCause("Неисправность датчика\n" +
                    "защиты от перегрева;\n" +
                    "Обрыв соединительного\n" +
                    "кабеля датчика перегрева;\n" +
                    "Неисправность системы\n" +
                    "защиты от перегрева.");
            error30.setSeriesTitle(seriesTitle2);
            //error30.setSeries("T32-36DK");
            errorRepository.save(error30);

            Error error31 = new Error();
            error31.setCode("Е3");
            error31.setDescription("Неисправность системы\n" +
                    "дымоудаления");
            error31.setCause("Неисправность\n" +
                    "вентилятора;\n" +
                    "Отказ прессостата (только\n" +
                    "модели без инверторной\n" +
                    "турбины);\n" +
                    "Засорение трубы\n" +
                    "дымоудаления.");
            error31.setSeriesTitle(seriesTitle2);
            //error31.setSeries("T32-36DK");
            errorRepository.save(error31);

            Error error32 = new Error();
            error32.setCode("Е4");
            error32.setDescription("Недостаточное давление\n" +
                    "теплоносителя в контуре\n" +
                    "отопления.");
            error32.setCause("Воздушная пробка в\n" +
                    "системе отопления;\n" +
                    "Неисправен датчик\n" +
                    "давления теплоносителя в\n" +
                    "контуре отопления;");
            error32.setSeriesTitle(seriesTitle2);
            //error32.setSeries("T32-36DK");
            errorRepository.save(error32);

            Error error33 = new Error();
            error33.setCode("Е5");
            error33.setDescription("Неисправность в напряжении\n" +
                    "электромагнитного клапана");
            error33.setCause("Плата управления выдает\n" +
                    "неправильное напряжение\n" +
                    "на электромагнитный клапан газового клапана.");
            error33.setSeriesTitle(seriesTitle2);
            //error33.setSeries("T32-36DK");
            errorRepository.save(error33);

            Error error34 = new Error();
            error34.setCode("Е6");
            error34.setDescription("Повреждение датчика\n" +
                    "температуры контура ГВС");
            error34.setCause("Повреждение датчика\n" +
                    "температуры (обрыв цепи,\n" +
                    "короткое замыкание);\n" +
                    "Обрыв соединительного\n" +
                    "кабеля датчика температуры.");
            error34.setSeriesTitle(seriesTitle2);
            //error34.setSeries("T32-36DK");
            errorRepository.save(error34);

            Error error35 = new Error();
            error35.setCode("Е7");
            error35.setDescription("Повреждение датчика\n" +
                    "температуры отопления");
            error35.setCause("Повреждение датчика\n" +
                    "температуры (обрыв цепи,\n" +
                    "короткое замыкание);\n" +
                    "Обрыв соединительного\n" +
                    "кабеля датчика температуры.");
            error35.setSeriesTitle(seriesTitle2);
            //error35.setSeries("T32-36DK");
            errorRepository.save(error35);

            Error error36 = new Error();
            error36.setCode("Е8");
            error36.setDescription("Перегрев (≥ 90°C)");
            error36.setCause("Неисправность датчика\n" +
                    "температуры;\n" +
                    "Неисправность системы\n" +
                    "защиты от перегрева.");
            error36.setSeriesTitle(seriesTitle2);
            //error36.setSeries("T32-36DK");
            errorRepository.save(error36);

            Error error37 = new Error();
            error37.setCode("Е9");
            error37.setDescription("Обледенение (≤1°C)");
            error37.setCause("Система отопления может\n" +
                    "быть заморожена");
            error37.setSeriesTitle(seriesTitle2);
            //error37.setSeries("T32-36DK");
            errorRepository.save(error37);


            ///////////////////////////
            ///////////Suari//////////
            /////////////////////////
            //////////ST////////////
            ///////////////////////

            seriesTitle3.setTitle("S13ST-S16ST");

            Error error38 = new Error();
            error38.setCode("ЕО");
            error38.setDescription("Неисправность датчика температуры воды");
            error38.setCause("");
            error38.setSeriesTitle(seriesTitle3);
            //error38.setSeries("ST");
            errorRepository.save(error38);

            Error error39 = new Error();
            error39.setCode("Е1");
            error39.setDescription("Нарушен контакт с датчиком наличия пламени или электрод датчика касается деталей горелки или\n" +
                    "находится вне зоны пламени – проверить датчик наличия пламени, проверить омывание датчика пламенем");
            error39.setCause("");
            error39.setSeriesTitle(seriesTitle3);
            //error39.setSeries("ST");
            errorRepository.save(error39);

            Error error40 = new Error();
            error40.setCode("Е2");
            error40.setDescription("Защита от остаточного горения. Если горелка после выключения воды продолжает гореть, газовый клапан\n" +
                    "прекращает подачу газа подачу газа");
            error40.setCause("");
            error40.setSeriesTitle(seriesTitle3);
            //error40.setSeries("ST");
            errorRepository.save(error40);

            Error error41 = new Error();
            error41.setCode("Е3");
            error41.setDescription("Защита от перегрева");
            error41.setCause("");
            error41.setSeriesTitle(seriesTitle3);
            //error41.setSeries("ST");
            errorRepository.save(error41);

            /*Error error42 = new Error();
            error42.setCode("Е3");
            error42.setDescription("Защита от перегрева");
            error42.setCause("");
            error42.setSeriesTitle(seriesTitle3);
            //error42.setSeries("ST");
            errorRepository.save(error42);*/

            Error error43 = new Error();
            error43.setCode("Е4");
            error43.setDescription("Неисправность датчика протока воды");
            error43.setCause("");
            error43.setSeriesTitle(seriesTitle3);
            //error43.setSeries("ST");
            errorRepository.save(error43);

            Error error44 = new Error();
            error44.setCode("Е5");
            error44.setDescription("Неисправность турбины, неисправность прессостата");
            error44.setCause("");
            error44.setSeriesTitle(seriesTitle3);
            //error44.setSeries("ST");
            errorRepository.save(error44);

            Error error45 = new Error();
            error45.setCode("Е6");
            error45.setDescription("Защита от перегрева:\n" +
                    "– когда температура воды на выходе выше 85 ℃ в течение 10 секунд, газовый\n" +
                    "водонагреватель перестает работать.");
            error45.setCause("");
            error45.setSeriesTitle(seriesTitle3);
            //error45.setSeries("ST");
            errorRepository.save(error45);

            Error error46 = new Error();
            error46.setCode("Е7");
            error46.setDescription("Неисправность электромагнитного клапана");
            error46.setCause("");
            error46.setSeriesTitle(seriesTitle3);
            //error46.setSeries("ST");
            errorRepository.save(error46);

            Error error47 = new Error();
            error47.setCode("Еn");
            error47.setDescription("Таймер:\n" +
                    "- Газовый водонагреватель оснащен таймером продолжительности работы.\n" +
                    "После непрерывной работы в течении 60 минут водонагреватель\n" +
                    "выключается");
            error47.setCause("");
            error47.setSeriesTitle(seriesTitle3);
            //error47.setSeries("ST");
            errorRepository.save(error47);
            /////////////////////////
            //////////FT////////////
            ///////////////////////

            seriesTitle4.setTitle("S12FT");

            Error error48 = new Error();
            error48.setCode("ЕО");
            error48.setDescription("Неисправность датчика температуры воды");
            error48.setCause("");
            error48.setSeriesTitle(seriesTitle4);
            //error48.setSeries("FT");
            errorRepository.save(error48);

            Error error49 = new Error();
            error49.setCode("Е1");
            error49.setDescription("Нарушен контакт с датчиком наличия пламени или электрод датчика касается деталей горелки или\n" +
                    "находится вне зоны пламени – проверить датчик наличия пламени, проверить омывание датчика пламенем");
            error49.setCause("");
            error49.setSeriesTitle(seriesTitle4);
            //error49.setSeries("FT");
            errorRepository.save(error49);

            Error error50 = new Error();
            error50.setCode("Е2");
            error50.setDescription("Защита от остаточного горения -- есть горелка после выключения воды продолжает гореть, газовый клапан\n" +
                    "прекращает подачу газа подачу газа");
            error50.setCause("");
            error50.setSeriesTitle(seriesTitle4);
            //error50.setSeries("FT");
            errorRepository.save(error50);

            Error error51 = new Error();
            error51.setCode("Е3");
            error51.setDescription("Защита от перегрева");
            error51.setCause("");
            error51.setSeriesTitle(seriesTitle4);
            //error51.setSeries("FT");
            errorRepository.save(error51);

            Error error52 = new Error();
            error52.setCode("Е4");
            error52.setDescription("Неисправность датчика протока воды");
            error52.setCause("");
            error52.setSeriesTitle(seriesTitle4);
            //error52.setSeries("FT");
            errorRepository.save(error52);

            Error error53 = new Error();
            error53.setCode("Е5");
            error53.setDescription("Неисправность турбины, неисправность прессостата");
            error53.setCause("");
            error53.setSeriesTitle(seriesTitle4);
            //error53.setSeries("FT");
            errorRepository.save(error53);

            Error error54 = new Error();
            error54.setCode("Е6");
            error54.setDescription("Защита от перегрева:\n" +
                    "– когда температура воды на выходе выше 85 ℃ в течение 10 секунд, газовый водонагреватель перестает работать.");
            error54.setCause("");
            error54.setSeriesTitle(seriesTitle4);
            //error54.setSeries("FT");
            errorRepository.save(error54);

            Error error55 = new Error();
            error55.setCode("Е7");
            error55.setDescription("Неисправность электромагнитного клапана");
            error55.setCause("");
            error55.setSeriesTitle(seriesTitle4);
            //error55.setSeries("FT");
            errorRepository.save(error55);

            Error error56 = new Error();
            error56.setCode("Еn");
            error56.setDescription("Таймер\n" +
                    "- Газовый водонагреватель оснащен таймером продолжительности работы. После непрерывной работы" +
                    " в течении 60 минут водонагреватель\n" +
                    "выключается.");
            error56.setCause("");
            error56.setSeriesTitle(seriesTitle4);
            //error56.setSeries("FT");
            errorRepository.save(error56);


            ///////////////////////////////////
            /////////////Ainova///////////////
            /////////////////////////////////
            //////////Серия LT-D////////////
            ///////////////////////////////

            seriesTitle5.setTitle("LT4-24D");
            Error error1 = new Error();
            error1.setCode("E0");
            error1.setDescription("Автоматическое восстановление после\n" +
                    "повышения температуры теплоносителя\n" +
                    "на 1°С");
            error1.setCause("Ошибка режима «АНТИЗАМОРОЗКА»");
            error1.setSeriesTitle(seriesTitle5);
            //error1.setSeries("LT-D");
            errorRepository.save(error1);

            Error error2 = new Error();
            error2.setCode("E1");
            error2.setDescription("Нажмите и удерживайте кнопку «Reset»\n" +
                    "для удаления ошибки");
            error2.setCause("Ошибка в электрической цепи котла");
            error2.setSeriesTitle(seriesTitle5);
            //error2.setSeries("LT-D");
            errorRepository.save(error2);

            Error error3 = new Error();
            error3.setCode("E3");
            error3.setDescription("Нажмите и удерживайте кнопку «Reset»\n" +
                    "для удаления ошибки");
            error3.setCause("Обрыв электрической цепи аварийного\n" +
                    "датчика температуры");
            error3.setSeriesTitle(seriesTitle5);
            //error3.setSeries("LT-D");
            errorRepository.save(error3);

            Error error4 = new Error();
            error4.setCode("E4");
            error4.setDescription("Нажмите и удерживайте кнопку «Reset»\n" +
                    "для удаления ошибки");
            error4.setCause("Перегрев датчика температуры линии\n" +
                    "подачи");
            error4.setSeriesTitle(seriesTitle5);
            //error4.setSeries("LT-D");
            errorRepository.save(error4);

            Error error5 = new Error();
            error5.setCode("E7");
            error5.setDescription("Автоматическое восстановление");
            error5.setCause("Ошибка датчика температуры\n" +
                    "теплоносителя");
            error5.setSeriesTitle(seriesTitle5);
            //error5.setSeries("LT-D");
            errorRepository.save(error5);

            Error error6 = new Error();
            error6.setCode("EА");
            error6.setDescription("Автоматическое восстановление");
            error6.setCause("Ошибка датчика реле протока.\n" +
                    "(фактический проток есть, но нет сигнала\n" +
                    "от датчика)");
            error6.setSeriesTitle(seriesTitle5);
            //error6.setSeries("LT-D");
            errorRepository.save(error6);

            Error error7 = new Error();
            error7.setCode("EР");
            error7.setDescription("Автоматическое восстановление");
            error7.setCause("Ошибка датчика реле протока\n" +
                    "(отсутствие циркуляция теплоносителя)");
            error7.setSeriesTitle(seriesTitle5);
            //error7.setSeries("LT-D");
            errorRepository.save(error7);
            /////////////////////////////////
            //////////Серия QM////////////
            ////////////////////////////////

            seriesTitle6.setTitle("QM4-24");
            Error error17 = new Error();
            error17.setCode("E0");
            error17.setDescription("Автоматическое восстановление");
            error17.setCause("Перегрев теплоносителя");
            error17.setSeriesTitle(seriesTitle6);
            //error17.setSeries("QM");
            errorRepository.save(error17);

            Error error18 = new Error();
            error18.setCode("E1");
            error18.setDescription("Автоматическое восстановление");
            error18.setCause("Обрыв электрической цепи датчика температуры NTC бойлера косвенного нагрева.");
            error18.setSeriesTitle(seriesTitle6);
            //error18.setSeries("QM");
            errorRepository.save(error18);

            Error error19 = new Error();
            error19.setCode("E2");
            error19.setDescription("Автоматическое восстановление");
            error19.setCause("Перегрев датчика температуры линии подачи.");
            error19.setSeriesTitle(seriesTitle6);
            //error19.setSeries("QM");
            errorRepository.save(error19);

            Error error20 = new Error();
            error20.setCode("E3");
            error20.setDescription("Автоматическое восстановление");
            error20.setCause("Обрыв электрической цепи датчика температуры линии подачи.");
            error20.setSeriesTitle(seriesTitle6);
            //error20.setSeries("QM");
            errorRepository.save(error20);

            Error error21 = new Error();
            error21.setCode("E3");
            error21.setDescription("Автоматическое восстановление");
            error21.setCause("Обрыв электрической цепи датчика температуры линии подачи.");
            error21.setSeriesTitle(seriesTitle6);
            //error21.setSeries("QM");
            errorRepository.save(error21);

            Error error22 = new Error();
            error22.setCode("E7");
            error22.setDescription("Автоматическое восстановление");
            error22.setCause("Перегрев датчика температуры линии обратки.");
            error22.setSeriesTitle(seriesTitle6);
            //error22.setSeries("QM");
            errorRepository.save(error22);

            Error error23 = new Error();
            error23.setCode("E8");
            error23.setDescription("Автоматическое восстановление");
            error23.setCause("Обрыв электрической цепи датчика температуры линии обратки.");
            error23.setSeriesTitle(seriesTitle6);
            //error23.setSeries("QM");
            errorRepository.save(error23);

            Error error24 = new Error();
            error24.setCode("EA");
            error24.setDescription("Нажмите и удерживайте кнопку ON/OFF для удаления ошибки");
            error24.setCause("Ошибка в электрической цепи котла.");
            error24.setSeriesTitle(seriesTitle6);
            //error24.setSeries("QM");
            errorRepository.save(error24);

            Error error25 = new Error();
            error25.setCode("EB");
            error25.setDescription("Нажмите и удерживайте кнопку ON/OFF для удаления ошибки");
            error25.setCause("Обрыв электрической цепи аварийного датчика температуры");
            error25.setSeriesTitle(seriesTitle6);
            //error25.setSeries("QM");
            errorRepository.save(error25);

            Error error26 = new Error();
            error26.setCode("EC");
            error26.setDescription("Нажмите и удерживайте кнопку ON/OFF для удаления ошибки");
            error26.setCause("Ошибка циркуляции теплоносителя");
            error26.setSeriesTitle(seriesTitle6);
            //error26.setSeries("QM");
            errorRepository.save(error26);

            Error error27 = new Error();
            error27.setCode("EF");
            error27.setDescription("Нажмите и удерживайте кнопку ON/OFF для удаления ошибки");
            error27.setCause("Ошибка датчика протока");
            error27.setSeriesTitle(seriesTitle6);
            //error27.setSeries("QM");
            errorRepository.save(error27);

            Error error28 = new Error();
            error28.setCode("EU");
            error28.setDescription("Нажмите и удерживайте кнопку ON/OFF для удаления ошибки");
            error28.setCause("Ошибка в работе панели управления");
            error28.setSeriesTitle(seriesTitle6);
            //error28.setSeries("QM");
            errorRepository.save(error28);
        }
    }
}
