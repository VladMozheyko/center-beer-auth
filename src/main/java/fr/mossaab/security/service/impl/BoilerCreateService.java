package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.*;
import fr.mossaab.security.enums.CategoryOfAdvantage;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoilerCreateService {
    @Autowired
    private TypeService typeService;
    @Autowired
    private KindService kindService;
    @Autowired
    private UnitService unitService;
    @Autowired
    private SeriesService seriesService;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private AttributeService attributeService;
    @Autowired
    private ImageForSeriesService imageForSeriesService;
    @Autowired
    private BoilerService boilerService;
    @Autowired
    private AdvantageService advantageService;
    @Autowired
    private AcceptableValueService acceptableValueService;

    @Autowired
    private TypeRepository typeRepository;
    @Autowired
    private KindRepository kindRepository;
    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private SeriesRepository seriesRepository;
    @Autowired
    private CharacteristicRepository characteristicRepository;
    @Autowired
    private AttributeRepository attributeRepository;
    @Autowired
    private ImageForSeriesRepository imageForSeriesRepository;
    @Autowired
    private BoilerRepository boilerRepository;
    @Autowired
    private AdvantageRepository advantageRepository;
    @Autowired
    private AcceptableValueRepository acceptableValueRepository;
    /*@Autowired
    private JdbcTemplate jdbcTemplate;
    */

    /*public void resetAutoIncrement() {
        typeRepository.deleteAll();
        String schemaSql = "USE vuary";
        jdbcTemplate.execute(schemaSql);
        String sql1 = "ALTER TABLE types AUTO_INCREMENT = 1";
        jdbcTemplate.execute(sql1);
        String sql2 = "ALTER TABLE _presentation AUTO_INCREMENT = 1";
        jdbcTemplate.execute(sql2);

        Type t1 = new Type("TOIVO", "Котлы настенные газовые", "image_toivo.png");
        typeRepository.save(t1);
        Type t2 = new Type("TOIVO", "Котлы настенные газовые", "image_toivo.png");
        typeRepository.save(t2);

    }*/
    public boolean CheckRepos() {
        return typeRepository.count() == 0 &&
                kindRepository.count() == 0 &&
                unitRepository.count() == 0 &&
                seriesRepository.count() == 0 &&
                characteristicRepository.count() == 0 &&
                attributeRepository.count() == 0 &&
                imageForSeriesRepository.count() == 0 &&
                boilerRepository.count() == 0 &&
                advantageRepository.count() == 0 &&
                acceptableValueRepository.count() == 0
                ;
    }

    public void CreateBoilers() {
        if (CheckRepos()) {
            // Типы котлов
            //------------------------------------------------------------------------------------------------------
            Type t1 = new Type("TOIVO", "Котлы настенные газовые", "image_toivo.png");
            Type t2 = new Type("SUARI", "Водонагреватели проточные газовые", "image_suari.png");
            Type t3 = new Type("AINOVA", "Котлы настенные электрические", "image_ainova.png");
            Type t4 = new Type("SALMI", "Водонагреватели электрические накопительные", "image_salmi.png");
            typeService.addAll(List.of(t1, t2, t3, t4));

            // Виды котлов
            //------------------------------------------------------------------------------------------------------
            Kind k1 = new Kind("Одноконтурные", "(с закрытой камерой) без трёхходового клапана", t1);
            Kind k2 = new Kind("Одноконтурные", "(с закрытой камерой) с трёхходовым клапаном", t1);
            Kind k3 = new Kind("Двухконтурные", "(с закрытой камерой)", t1);
            Kind k4 = new Kind("Классические модели", "(с механической регулировкой)", t2);
            Kind k5 = new Kind("Классические модели", "(с электронной модуляцией пламени)", t2);
            Kind k6 = new Kind("Полутурбо", "(дымоход в комплекте)", t2);
            Kind k7 = new Kind("Турбо", "(дымоход в комплекте)", t2);
            Kind k8 = new Kind("Стандарт", "", t3);
            Kind k9 = new Kind("Миникотельные", "", t3);
            Kind k10 = new Kind("Малый литраж", "", t4);
            Kind k11 = new Kind("Круглые эконом", "", t4);
            Kind k12 = new Kind("Круглые комфорт", "", t4);
            Kind k13 = new Kind("Плоские комфорт", "", t4);
            Kind k14 = new Kind("Плоские премиум", "", t4);
            Kind k15 = new Kind("Большой литраж", "", t4);

            kindService.addAll(List.of(k1, k2, k3, k4, k5, k6, k7, k8, k9, k10, k11, k12, k13, k14, k15));

            // Единиицы измерения
            //------------------------------------------------------------------------------------------------------
            Unit u1 = new Unit("кВт", "Киловатт");
            Unit u2 = new Unit("%", "Процентов");
            Unit u3 = new Unit("м³/ч", "Кубических метров в час");
            Unit u4 = new Unit("бар", "");
            Unit u5 = new Unit("л", "Литров");
            Unit u6 = new Unit("℃", "Градусов Цельсия");
            Unit u7 = new Unit("дюйм", "Дюймов");
            Unit u8 = new Unit("В/Гц", "Вольт/Герц");
            Unit u9 = new Unit("Вт", "Ватт");
            Unit u10 = new Unit("мм", "Миллиметров");
            Unit u11 = new Unit("кг", "Килограмм");
            Unit u12 = new Unit("Па", "Паскалей");
            Unit u13 = new Unit("кг/ч", "Килограмм в час");
            Unit u14 = new Unit("МПа", "Мегапаскалей");
            Unit u15 = new Unit("л/мин", "Литров в минуту");
            Unit u16 = new Unit("м³", "Кубических метров");
            Unit u17 = new Unit("м³", "Кубических метров");
            Unit u18 = new Unit("IP", "Степень защиты");
            Unit u19 = new Unit("", "");
            Unit u20 = new Unit("B", "Вольт");
            Unit u21 = new Unit("м³", "Метров кубических");
            Unit u22 = new Unit("мм²", "Миллиметров квадратных");
            Unit u23 = new Unit("Гц", "Герц");




            unitService.addAll(List.of(u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16, u17, u18, u19, u20, u21, u22, u23));

            // Серии котлов
            //------------------------------------------------------------------------------------------------------
            Series s1 = new Series("T10O-T24O",
                    "Одноконтурные котелы, с закрытой камерой сгорания, без трехходового клапана, модели (T10O-T24O)", k1);
            Series s2 = new Series("T10OK-T24OK",
                    "Одноконтурные котелы, с закрытой камерой сгорания, с трёхходовым клапаном, модели (T10OK-T24OK)", k2);
            Series s3 = new Series("T10DK-T24DK","Двухконтурные котелы, с закрытой камерой сгорания, модели (T10DK-T24DK)", k3);
            Series s4 = new Series("S10-S12","Классические модели, с механической регулировкой, модели (S10-S12)", k4);
            Series s5 = new Series("S10EM-S12EM","Классические модели, с электронной модуляцией пламени, модели (S10EM-S12EM)", k5);
            Series s6 = new Series("S13ST-S16ST","Полутурбо, дымоход в комплекте, модели (S13ST-S16ST)", k6);
            Series s7 = new Series("S12FT","Турбо, дымоход в комплекте, модели (S12FT)", k7);
            Series s8 = new Series("LT4-24D","Стандарт, модели (LT4-24D)", k8);
            Series s9 = new Series("QM4-24","Миникотельные, модели (QM4-24)", k9);
            Series s10 = new Series("KMU10I KMU15I KMU30I","Малый литраж, модели (KMU10I KMU15I KMU30I)", k10);
            Series s11 = new Series("VRM30 VRM50 VRM80 VRM100","Круглые эконом, модели (VRM30 VRM50 VRM80 VRM100)", k11);
            Series s12 = new Series("VRM30D VRM50D VRM80D VRM100D","Круглые комфорт, модели (VRM30D VRM50D VRM80D VRM100D)", k12);
            Series s13 = new Series("VFM30D VFM50D VFM80D","Плоские комфорт, модели (VFM30D VFM50D VFM80D)", k13);
            Series s14 = new Series("VFE30WE VFE50WE VFE80WE","Плоские премиум, модели (VFE30WE VFE50WE VFE80WE)", k14);
            Series s15 = new Series("FRM200 FRM300","Большой литраж, модели (FRM200 FRM300)", k15);

            seriesService.addAll(List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15));

            // Изображения котлов
            //------------------------------------------------------------------------------------------------------
            ImageForSeries i1 = new ImageForSeries("T18O_01.png", "image/*", "/var/www/vuary/Series/T18O_01.png", s1);
            ImageForSeries i2 = new ImageForSeries("T24OK_01.png", "image/*", "/var/www/vuary/Series/T24OK_01.png", s2);
            ImageForSeries i3 = new ImageForSeries("T24DK_01.png", "image/*", "/var/www/vuary/Series/T24DK_01.png", s3);
            ImageForSeries i4 = new ImageForSeries("S12_01.png", "image/*", "/var/www/vuary/Series/S12_01.png", s4);
            ImageForSeries i5 = new ImageForSeries("S10EM_01.png", "image/*", "/var/www/vuary/Series/S10EM_01.png", s5);
            ImageForSeries i6 = new ImageForSeries("S13ST_01.png", "image/*", "/var/www/vuary/Series/S13ST_01.png", s6);
            ImageForSeries i7 = new ImageForSeries("S12FT_01.png", "image/*", "/var/www/vuary/Series/S12FT_01.png", s7);
            ImageForSeries i8 = new ImageForSeries("LT4_24D.png", "image/*", "/var/www/vuary/Series/LT4_24D.png", s8);
            ImageForSeries i9 = new ImageForSeries("QM-4_06_2.png", "image/*", "/var/www/vuary/Series/QM-4_06_2.png", s9);
            ImageForSeries i10 = new ImageForSeries("KMU_10_1.png", "image/*", "/var/www/vuary/Series/KMU-10_1.png", s10);
            ImageForSeries i11 = new ImageForSeries("VRM_50_1.png", "image/*", "/var/www/vuary/Series/VRM-50_1.png", s11);
            ImageForSeries i12 = new ImageForSeries("VRM_50D_01.png", "image/*", "/var/www/vuary/Series/VRM-50D_01.png", s12);
            ImageForSeries i13 = new ImageForSeries("VFM_50D_01.png", "image/*", "/var/www/vuary/Series/VFM-50D_01.png", s13);
            ImageForSeries i14 = new ImageForSeries("VFE_50WE_01.png", "image/*", "/var/www/vuary/Series/VFE-50WE_01.png", s14);
            ImageForSeries i15 = new ImageForSeries("FRM_200_300.png", "image/*", "/var/www/vuary/Series/FRM_200_300.png", s15);

            imageForSeriesService.addAll(List.of(i1, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13, i14, i15));

            // Характеристики
            //------------------------------------------------------------------------------------------------------
            Characteristic c1 = new Characteristic(
                    "Макс./мин. тепловая мощность в режиме отопление",
                    u1);
            Characteristic c2 = new Characteristic(
                    "Макс./мин. теплопроизводительность",
                    u1);
            Characteristic c3 = new Characteristic(
                    "КПД не менее",
                    u2);
            Characteristic c4 = new Characteristic(
                    "Максимальный расход природного газа",
                    u3);
            Characteristic c5 = new Characteristic(
                    "Давление в воздушной полости расширительного бака",
                    u4);
            Characteristic c6 = new Characteristic(
                    "Объем расширительного бака",
                    u5);
            Characteristic c7 = new Characteristic(
                    "Давление в системе отопления",
                    u4);
            Characteristic c8 = new Characteristic(
                    "Диапазон регулировки температуры теплоносителя",
                    u6);
            Characteristic c9 = new Characteristic(
                    "Диапазон регулировки температуры бытовой горячей воды (бойлер косвенного нагрева)",
                    u6);
            Characteristic c10 = new Characteristic(
                    "Макс. давление в контуре бойлера косвенного нагрева)",
                    u4);
            Characteristic c11 = new Characteristic(
                    "Присоединительный размер газовой магистрали)",
                    u7);
            Characteristic c12 = new Characteristic(
                    "Патрубки подключения подающей и обратной линий системы отопления)",
                    u7);
            Characteristic c13 = new Characteristic(
                    "Патрубки подключения холодной воды и бойлера косвенного нагрева)",
                    u7);
            Characteristic c14 = new Characteristic(
                    "Номинальное напряжение/частота)",
                    u8);
            Characteristic c15 = new Characteristic(
                    "Потребляемая эл. мощность)",
                    u9);
            Characteristic c16 = new Characteristic(
                    "Присоединительный размер дымохода)",
                    u10);
            Characteristic c17 = new Characteristic(
                    "Класс и уровень защиты)",
                    u18);
            Characteristic c18 = new Characteristic(
                    "Вес, нетто)",
                    u11);
            Characteristic c19 = new Characteristic(
                    "Габаритные размеры)",
                    u10);
            Characteristic c20 = new Characteristic(
                    "Номинальная тепловая мощность)",
                    u1);
            Characteristic c21 = new Characteristic(
                    "Номинальная теплопроизводительность)",
                    u1);
            Characteristic c22 = new Characteristic(
                    "Номинальное давление газа)",
                    u19);
            Characteristic c23 = new Characteristic(
                    "Номинальный расход газа)",
                    u3);
            Characteristic c24 = new Characteristic(
                    "Давление подводимой воды для нормальной работы аппарата)",
                    u14);
            Characteristic c25 = new Characteristic(
                    "Расход воды при нагреве на ΔT = 25°C)",
                    u15);
            Characteristic c26 = new Characteristic(
                    "Тип и напряжение элементов питания)",
                    u20);
            Characteristic c27 = new Characteristic(
                    "Присоединительные размеры:",
                    u19);
            Characteristic c30 = new Characteristic(
                    "Диаметр дымохода)",
                    u10);
            Characteristic c31 = new Characteristic(
                    "Тип камеры сгорания)",
                    u19);
            Characteristic c32 = new Characteristic(
                    "Тип дымоудаления)",
                    u19);
            Characteristic c33 = new Characteristic(
                    "Наличие модуляции пламени)",
                    u19);
            Characteristic c34 = new Characteristic(
                    "Расход газа)",
                    u21);
            Characteristic c35 = new Characteristic(
                    "Мин. расход воды, необходимый для зажигания горелки)",
                    u15);
            Characteristic c36 = new Characteristic(
                    "Напряжение и частота)",
                    u8);
            Characteristic c37 = new Characteristic(
                    "Зажигание)",
                    u19);
            Characteristic c39 = new Characteristic(
                    "Модель)",
                    u19);
            Characteristic c40 = new Characteristic(
                    "Номинальная потребляемая мощность)",
                    u1);
            Characteristic c41 = new Characteristic(
                    "Количество ступеней мощности)",
                    u19);
            Characteristic c43 = new Characteristic(
                    "Номинальный ток автоматического выключателя)",
                    u19);
            Characteristic c44 = new Characteristic(
                    "Сечение токопроводящей жилы)",
                    u22);
            Characteristic c45 = new Characteristic(
                    "Рабочее давление теплоносителя)",
                    u14);
            Characteristic c46 = new Characteristic(
                    "Тип системы отопления)",
                    u19);
            Characteristic c47 = new Characteristic(
                    "Диапазон регулирования температуры теплоносителя)",
                    u6);
            Characteristic c48 = new Characteristic(
                    "Диапазон регулирования температуры воды)",
                    u6);
            Characteristic c49 = new Characteristic(
                    "Класс влагозащищенности)",
                    u18);
            Characteristic c50 = new Characteristic(
                    "Циркуляционный насос)",
                    u19);
            Characteristic c51 = new Characteristic(
                    "Габаритные размеры, В*Ш*Г не более)",
                    u10);
            Characteristic c52 = new Characteristic(
                    "Масса нетто, не более)",
                    u11);
            Characteristic c56 = new Characteristic(
                    "Макс./мин. тепловая мощность в режиме ГВС)",
                    u1);
            Characteristic c57 = new Characteristic(
                    "Макс./мин. давления в контуре ГВС",
                    u4);
            Characteristic c58 = new Characteristic(
                    "Диапазон регулировки температуры бытовой горячей воды)",
                    u6);
            Characteristic c59 = new Characteristic(
                    "Производительность по нагреву горячей воды （ при △T=25℃）)",
                    u15);
            Characteristic c60 = new Characteristic(
                    "Производительность по нагреву горячей воды（при △T=30℃）",
                    u15);
            Characteristic c61 = new Characteristic(
                    "Минимальный пусковой напор воды",
                    u15);
            Characteristic c62 = new Characteristic(
                    "Присоединительный размер газовой магистрали",
                    u7);
            Characteristic c63 = new Characteristic(
                    "Патрубки подключения холодной и горячей воды",
                    u7);
            Characteristic c64 = new Characteristic(
                    "Объем",
                    u5);
            Characteristic c65 = new Characteristic(
                    "Вес нетто/брутто",
                    u11);
            Characteristic c66 = new Characteristic(
                    "Макс. раб. давление,",
                    u14);
            Characteristic c67 = new Characteristic(
                    "Номин. напряжение,",
                    u19);
            Characteristic c68 = new Characteristic(
                    "Номин. мощность",
                    u1);
            Characteristic c69 = new Characteristic(
                    "Класс защиты",
                    u19);
            Characteristic c70 = new Characteristic(
                    "Время нагрева",
                    u19);
            Characteristic c71 = new Characteristic(
                    "Номинальное давление природного газа",
                    u12);
            Characteristic c72 = new Characteristic(
                    "Номинальное давление сжиженного газа",
                    u12);
            Characteristic c73 = new Characteristic(
                    "Номинальный расход природного газа",
                    u3);
            Characteristic c74 = new Characteristic(
                    "Номинальный расход сжиженного газа",
                    u13);
            Characteristic c75 = new Characteristic(
                    "Присоединительные размеры: вход холодной воды",
                    u20);
            Characteristic c76 = new Characteristic(
                    "Присоединительные размеры: выход горячей воды",
                    u20);
            Characteristic c77 = new Characteristic(
                    "Присоединительные размеры: вход газа",
                    u20);
            Characteristic c78 = new Characteristic(
                    "Номинальная частота",
                    u23);
            Characteristic c79 = new Characteristic(
                    "Располо- жение патрубков",
                    u19);
            Characteristic c80 = new Characteristic("Номинальное напряжение", u20);


            // TODO ждем ответа по пункту Время нагрева * (∆45°C) документа Passport_FRM_A5+3mm_new.pdf

            c79.setSeries(List.of(s10));
            c1.setSeries(List.of(s1));
            c2.setSeries(List.of(s1, s2, s3));
            c3.setSeries(List.of(s1, s2, s3, s4, s5, s7));
            c4.setSeries(List.of(s1, s2, s3));
            c5.setSeries(List.of(s1, s2, s3));
            c6.setSeries(List.of(s1, s2, s3, s9));
            c7.setSeries(List.of(s1, s2, s3));
            c8.setSeries(List.of(s1, s2, s3));
            c9.setSeries(List.of(s1, s2));
            c10.setSeries(List.of(s1, s2));
            c11.setSeries(List.of(s1, s2));
            c12.setSeries(List.of(s1, s2, s3));
            c13.setSeries(List.of(s1, s2));
            c14.setSeries(List.of(s1, s2, s3));
            c15.setSeries(List.of(s1, s2, s3));
            c16.setSeries(List.of(s1, s2, s3));
            c17.setSeries(List.of(s1, s2, s3));
            c18.setSeries(List.of(s1, s2, s3));
            c19.setSeries(List.of(s1, s2, s3, s4, s5,s8, s10, s11, s12, s13, s14, s15));
            c20.setSeries(List.of(s4, s5, s7));
            c21.setSeries(List.of(s4, s5));
            c22.setSeries(List.of(s4, s5, s7));
            c23.setSeries(List.of(s4, s5));
            c24.setSeries(List.of(s4, s5, s7));
            c25.setSeries(List.of(s4, s5, s7));
            c26.setSeries(List.of(s4, s5));
            c27.setSeries(List.of(s4, s5, s7, s8, s9));

            c30.setSeries(List.of(s4, s5));
            c31.setSeries(List.of(s7));
            c32.setSeries(List.of(s1, s2, s3, s7));
            c33.setSeries(List.of(s7));
            c34.setSeries(List.of(s7));
            c35.setSeries(List.of(s7));
            c36.setSeries(List.of(s7));
            c37.setSeries(List.of(s7));
            c39.setSeries(List.of(s9));
            c40.setSeries(List.of(s9,s8));
            c41.setSeries(List.of(s9,s8));
            c43.setSeries(List.of(s9,s8));
            c44.setSeries(List.of(s9,s8));
            c45.setSeries(List.of(s9,s8));
            c46.setSeries(List.of(s9,s8));
            c47.setSeries(List.of(s9,s8));
            c48.setSeries(List.of(s9));
            c49.setSeries(List.of(s9,s8));
            c50.setSeries(List.of(s9));
            c51.setSeries(List.of(s9));
            c52.setSeries(List.of(s9,s8));
            c56.setSeries(List.of(s3));
            c57.setSeries(List.of(s3));
            c58.setSeries(List.of(s3));
            c59.setSeries(List.of(s3));
            c60.setSeries(List.of(s3));
            c61.setSeries(List.of(s3));
            c62.setSeries(List.of(s3));
            c63.setSeries(List.of(s3));
            c64.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            c65.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            c66.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            c67.setSeries(List.of(s9, s10, s11, s12, s13, s14, s15));
            c68.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            c69.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            c70.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            c71.setSeries(List.of(s4, s5, s7));
            c72.setSeries(List.of(s4, s5, s7));
            c73.setSeries(List.of(s4, s5, s7));
            c74.setSeries(List.of(s4, s5, s7));
            c75.setSeries(List.of(s9));
            c76.setSeries(List.of(s9));
            c77.setSeries(List.of(s9));
            c78.setSeries(List.of(s9,s8));
            c80.setSeries(List.of(s8));

            characteristicService.addAll(List.of(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18, c19,
                    c20, c21, c22, c23, c24, c25, c26, c27, c30, c31, c32, c33, c34, c35, c36, c37, c39, c40, c41,
                    c43, c44, c45, c46, c47, c48, c49, c50, c51, c52, c56, c57, c58, c59, c60, c61, c62, c63, c64, c65, c66, c67, c68, c69, c70, c71, c72, c73, c74, c75, c76, c77, c78, c79,c80));

            // Аттрибуты
            //------------------------------------------------------------------------------------------------------
            Attribute a1 = new Attribute("Высококачественная латунная гидрогруппа", s1);
            Attribute a2 = new Attribute("Газовый клапан Sit 845 Sigma", s1);
            Attribute a3 = new Attribute("Инверторный привод турбины", s1);
            Attribute a4 = new Attribute("Медные патрубки", s1);
            Attribute a5 = new Attribute("Цифровой датчик давления теплоносителя", s1);
            Attribute a6 = new Attribute("Высококачественная латунная гидрогруппа", s2);
            Attribute a7 = new Attribute("Газовый клапан Sit 845 Sigma", s2);
            Attribute a8 = new Attribute("Инверторный привод турбины", s2);
            Attribute a9 = new Attribute("Медные патрубки", s2);
            Attribute a10 = new Attribute("Цифровой датчик давления теплоносителя", s2);
            Attribute a11 = new Attribute("Датчик бойлера в комплекте", s2);
            Attribute a12 = new Attribute("Высококачественная латунная гидрогруппа", s3);
            Attribute a13 = new Attribute("Газовый клапан Sit 845 Sigma", s3);
            Attribute a14 = new Attribute("Инверторный привод турбины", s3);
            Attribute a15 = new Attribute("Медные патрубки", s3);
            Attribute a16 = new Attribute("Цифровой датчик давления теплоносителя", s3);
            Attribute a17 = new Attribute("Вторичный теплообменник имеет 12 пластин", s3);
            Attribute a18 = new Attribute("Классическая модель, механическая регулировка температуры", s4);
            Attribute a19 = new Attribute("Cистема электронной модуляции (EM) пламени горелки позволяет более точно поддерживать заданную температуру горячей воды в пределах 1°C вне зависимости от увеличения/уменьшения протока.", s5);
            Attribute a20 = new Attribute("В моделях «Полутурбо» с открытой камерой сгорания воздух для горения поступает из помещения, в котором установлены водонагреватель, а дымовые газы отводятся при помощи вентилятора и дымовой трубы наружу", s6);
            Attribute a21 = new Attribute("В моделях «Турбо» с закрытой камерой сгорания турбина доставляет уличный воздух для горения и выводит наружу продукты сгорания при помощи коаксиального дымохода", s7);
            Attribute a22 = new Attribute(" ", s8);
            Attribute a23 = new Attribute("Встроенный циркуляционный насос 15-PBG-6 с возможностью настройки выбега", s9);
            Attribute a24 = new Attribute("Расширительный бак объёмом 6 литров", s9);
            Attribute a25 = new Attribute("Нагревательный модуль в теплоизоляции.", s9);
            Attribute a26 = new Attribute("Аналоговый манометр", s9);
            Attribute a27 = new Attribute("Кран подпитки", s9);
            Attribute a28 = new Attribute("Круглые накопительные водонагреватели малого объёма с верхним подключением к сети водоснабжения ", s10);
            Attribute a29 = new Attribute("Круглые накопительные водонагреватели", s11);
            Attribute a30 = new Attribute("Круглые накопительные водонагреватели с цифровым дисплеем", s12);
            Attribute a31 = new Attribute("Плоские накопительные водонагреватели повышенной мощности с цифровым управлением", s13);
            Attribute a32 = new Attribute("Плоские накопительные водонагреватели повышенной мощности с управлением по Wi-Fi", s14);
            Attribute a33 = new Attribute("Мощные накопительные водонагреватели большого объёма", s15);

            attributeService.addAll(List.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16,
                    a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33));

            // Котлы
            //------------------------------------------------------------------------------------------------------
            Boiler b1 = new Boiler("T10O", s1);
            Boiler b2 = new Boiler("T24O", s1);
            Boiler b3 = new Boiler("T10OK", s2);
            Boiler b4 = new Boiler("T24OK", s2);
            Boiler b5 = new Boiler("T10DK", s3);
            Boiler b6 = new Boiler("T24DK", s3);
            Boiler b7 = new Boiler("S10", s4);
            Boiler b8 = new Boiler("S12", s4);
            Boiler b9 = new Boiler("S10EM", s5);
            Boiler b10 = new Boiler("S12EM", s5);

            Boiler b11 = new Boiler("S13ST", s6);
            Boiler b12 = new Boiler("S16ST", s6);
            Boiler b13 = new Boiler("S12FT", s7);
            Boiler b14 = new Boiler("LT4", s8);
            Boiler b15 = new Boiler("24D", s8);
            Boiler b16 = new Boiler("QM4", s9);
            Boiler b17 = new Boiler("QM24", s9);

            Boiler b18 = new Boiler("KMU10I", s10);
            Boiler b19 = new Boiler("KMU15I", s10);
            Boiler b20 = new Boiler("VRM30", s11);
            Boiler b21 = new Boiler("VRM100", s11);
            Boiler b22 = new Boiler("VRM30D", s12);
            Boiler b23 = new Boiler("VRM100D", s12);
            Boiler b24 = new Boiler("VFM30D", s13);
            Boiler b25 = new Boiler("VFM80D", s13);
            Boiler b26 = new Boiler("VFE30WE", s14);
            Boiler b27 = new Boiler("VFE80WE", s14);
            Boiler b28 = new Boiler("FRM200", s15);
            Boiler b29 = new Boiler("FRM300", s15);

            boilerService.addAll(List.of(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17,
                    b18, b19, b20, b21, b22, b23, b24, b25, b26, b27, b28, b29));

            // Преимущества
            //------------------------------------------------------------------------------------------------------
            Advantage ad1 = new Advantage("Режим работы с теплыми полами", "Operating_mode_with_heated_floors.png", CategoryOfAdvantage.COMFORT);
            Advantage ad2 = new Advantage("Возможность работы на сжиженном газе", "The_ability_to_work_on_liquefied_gas.png", CategoryOfAdvantage.COMFORT);
            Advantage ad3 = new Advantage("Низкий уровень шума", "Low_noise_level.png", CategoryOfAdvantage.COMFORT);
            Advantage ad4 = new Advantage("Цифровое управление", "Digital_control.png", CategoryOfAdvantage.COMFORT);
            Advantage ad5 = new Advantage("Высокая экономичность", "High_efficiency.png", CategoryOfAdvantage.COMFORT);
            Advantage ad6 = new Advantage("Автоматическое электронное зажигание", "Automatic_electronic_ignition.png", CategoryOfAdvantage.COMFORT);
            Advantage ad7 = new Advantage("Регулировка температуры нагрева", "Adjustment_heating_temperature.png", CategoryOfAdvantage.COMFORT);
            Advantage ad8 = new Advantage("Работа при низком давлении воды", "Job_at_low_water_pressure.png", CategoryOfAdvantage.COMFORT);
            Advantage ad9 = new Advantage("Точное поддержание температуры", "Accurate_maintaining_temperature.png", CategoryOfAdvantage.COMFORT);
            Advantage ad10 = new Advantage("Сенсорная панель управления", "Sensory_control_Panel.png", CategoryOfAdvantage.COMFORT);
            Advantage ad11 = new Advantage("Различные режимы работы", "Various_modes_work.png", CategoryOfAdvantage.COMFORT);
            Advantage ad12 = new Advantage("Управление по Wi-Fi", "Wi_Fi_control.png", CategoryOfAdvantage.COMFORT);
            Advantage ad13 = new Advantage("Индикатор нагрева", "Heating_indicator.png", CategoryOfAdvantage.COMFORT);
            Advantage ad14 = new Advantage("Оптимальная мощность", "Optimal_power.png", CategoryOfAdvantage.COMFORT);
            Advantage ad15 = new Advantage("Низкие теплопотери", "Low_heat_loss.png", CategoryOfAdvantage.COMFORT);
            Advantage ad16 = new Advantage("Повышенная мощность", "Increased_power_2.png", CategoryOfAdvantage.COMFORT);
            Advantage ad17 = new Advantage("Повышенная мощность", "Increased_power_3.png", CategoryOfAdvantage.COMFORT);
            Advantage ad18 = new Advantage("Гарантия на бак", "Guarantee_five_years.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad19 = new Advantage("Магниевый анод", "Magnesium_anode.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad20 = new Advantage("Рабочее давление", "Working_pressure.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad21 = new Advantage("Высокое качество сварных швов", "High_quality_welds.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad22 = new Advantage("Нагревательный элемент из нержавеющей стали", "A_heating_element_stainless_steel.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad23 = new Advantage("Качественные комплектующие", "Quality_components.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad24 = new Advantage("Разъёмы для подключения внешних устройств", "Connectors_to_connect_external_devices.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad25 = new Advantage("Сухой нагревательный элемент", "Dry_heating_element.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad26 = new Advantage("Нижнее подключение", "Lower_connection.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad27 = new Advantage("Гарантия два года", "Two_year_warranty.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad28 = new Advantage("Горелка из нержавеющей стали", "Burner_stainless_become.png", CategoryOfAdvantage.COMFORT);
            Advantage ad29 = new Advantage("Теплообменник из бескислородной меди", "Heat_exchanger_from_oxygen_free_copper.png", CategoryOfAdvantage.COMFORT);
            Advantage ad30 = new Advantage("Жаропрочная эмаль", "Heat_resistant_enamel.png", CategoryOfAdvantage.COMFORT);
            Advantage ad31 = new Advantage("Высокая ремонтопригодность", "High_maintainability.png", CategoryOfAdvantage.COMFORT);
            Advantage ad32 = new Advantage("Защита от перегрева", "Double_overheat_protection.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad33 = new Advantage("Защита от избыточного давления", "Pressure_safety_valve_protects_against_excess_water.png", CategoryOfAdvantage.PROTECTION);
            //Advantage ad34 = new Advantage("Термостат – защита от перегрева, отключает подачу газа", "Thermostat_protection_against_overheating_disconne", CategoryOfAdvantage.PROTECTION);
            Advantage ad35 = new Advantage("Электрод ионизации – отключение подачи газа при отсутствии пламени на горелке", "Ionization_electrode_shutdown_gas_supply_when_there_is_no_flame_on_the_burner.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad36 = new Advantage("Датчик контроля тяги – защита при недостаточной или обратной тяге", "Traction_control_sensor_protection_in_case_of_insuffi.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad37 = new Advantage("Датчик протока – контроль напора воды", "Flow_switch.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad38 = new Advantage("Обратный клапан – защита системы от замерзания", "Check_valve_protection_anti_freeze_systems.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad39 = new Advantage("Защита от утечки тока", "Residual_current_device_included.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad40 = new Advantage("Автоматический воздухоотводчик", "Auto_air_vent.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad41 = new Advantage("Индикация ошибок", "Error_indication.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad42 = new Advantage("Двойной слой эмали на внутреннем баке – эффективная защита от коррозии", "Double_layer_of_enamel_on_the_inside_tank_effective_protection_against_corrosion.png", CategoryOfAdvantage.PROTECTION);

            Advantage ad43 = new Advantage("Встроенный суточный программатор", "Built-in_daily_programmer.png", CategoryOfAdvantage.COMFORT);
            Advantage ad44 = new Advantage("Hot Restart память параметров", "Hot_Restart_param_memory.png", CategoryOfAdvantage.COMFORT);
            Advantage ad45 = new Advantage("Защита от замерзания", "Freeze_protection.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad46 = new Advantage("Защита от срыва пламени", "Flame_failure_protection.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad47 = new Advantage("Защита от перепадов напряжения", "Voltage_fluctuation_protection.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad48 = new Advantage("Защита от сухого хода", "Dry_operation_protection.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad49 = new Advantage("Гарантия 3 года", "3_years_warranty.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad50 = new Advantage("Стойкость к накипи и коррозии", "Scale_and_corrosion_resistance.png", CategoryOfAdvantage.CONSTRUCTION);
            Advantage ad51 = new Advantage("Высокая производительность горячей воды", "High_hot_water_production_capacity.png", CategoryOfAdvantage.COMFORT);
            Advantage ad52 = new Advantage("Предохранительный клапан давления – защита от избыточного давления воды", "Pressure_safety_valve_protection_against_excessive_water_pressure.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad53 = new Advantage("Реле протока", "Flow_switch.png", CategoryOfAdvantage.PROTECTION);
            Advantage ad54 = new Advantage("ТЭН и колба из нержавеющей стали", "Stainless_steel_heating_element_and_tank.png", CategoryOfAdvantage.CONSTRUCTION);

            //TODO соед с сериями
            ad1.setSeries(List.of(s1, s2, s3, s8, s9));
            ad2.setSeries(List.of(s1, s2, s3, s4, s5, s6, s7));
            ad3.setSeries(List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15));
            ad4.setSeries(List.of(s1, s3, s4, s5, s6, s7, s8, s9, s11, s13));
            ad5.setSeries(List.of(s1, s2, s3));
            ad6.setSeries(List.of(s4, s5, s6, s7));
            ad7.setSeries(List.of(s4, s5, s6, s7, s10, s11, s12, s13, s14, s15));
            ad8.setSeries(List.of(s4, s5, s6, s7));
            ad9.setSeries(List.of(s5, s6, s7));
            ad10.setSeries(List.of(s6, s7, s14));
            ad11.setSeries(List.of(s9));
            ad12.setSeries(List.of(s9));
            ad13.setSeries(List.of(s10, s11, s12, s13, s15));
            ad14.setSeries(List.of(s10, s11, s12));
            ad15.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            ad16.setSeries(List.of(s13, s14));
            ad17.setSeries(List.of(s15));
            ad18.setSeries(List.of(s11, s12, s13, s14, s15));
            ad19.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            ad20.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            ad21.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            ad22.setSeries(List.of(s10, s11, s12, s13, s14, s15));
            ad23.setSeries(List.of(s1, s2, s3, s8, s9));
            ad24.setSeries(List.of(s9));
            ad25.setSeries(List.of(s9));
            ad26.setSeries(List.of(s8, s9));
            ad27.setSeries(List.of(s4, s5, s6, s7, s8));
            ad28.setSeries(List.of(s4, s5, s6, s7));
            ad29.setSeries(List.of(s1, s2, s3, s4, s5, s6, s7));
            ad30.setSeries(List.of(s4, s5, s6, s7));
            ad31.setSeries(List.of(s1, s2, s3, s4, s5, s6, s7));
            ad32.setSeries(List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15));
            ad33.setSeries(List.of(s1, s2, s3, s4, s5, s6, s7));
            //ad34.setSeries(List.of(s4, s5, s6, s7));
            ad35.setSeries(List.of(s4, s5, s6, s7));
            ad36.setSeries(List.of(s4, s5, s6, s7));
            ad37.setSeries(List.of(s5, s6, s7));
            ad38.setSeries(List.of(s7));
            ad39.setSeries(List.of(s8, s9));
            ad40.setSeries(List.of(s8, s9));
            ad41.setSeries(List.of(s8, s9));
            ad42.setSeries(List.of(s10, s11, s12, s13, s14, s15));

            ad43.setSeries(List.of(s1, s2, s3));
            ad44.setSeries(List.of(s1, s2, s3));
            ad45.setSeries(List.of(s1, s2, s3));
            ad46.setSeries(List.of(s1, s2, s3));
            ad47.setSeries(List.of(s1, s2, s3));
            ad48.setSeries(List.of(s1, s2, s3));
            ad49.setSeries(List.of(s1, s2, s3, s10));
            ad50.setSeries(List.of(s1, s2, s3));
            ad51.setSeries(List.of(s2, s3));
            ad52.setSeries(List.of(s4, s5, s6, s7));
            ad53.setSeries(List.of(s8, s9));
            ad54.setSeries(List.of(s8));


            advantageService.addAll(List.of(ad1, ad2, ad3, ad4, ad5, ad6, ad7, ad8, ad9, ad10, ad11, ad12, ad13, ad14, ad15,
                    ad16, ad17, ad18, ad19, ad20, ad21, ad22, ad23, ad24, ad25, ad26, ad27, ad28, ad29, ad30, ad31, ad32, ad33, ad35,
                    ad36, ad37, ad38, ad39, ad40, ad41, ad42));

            // Допустимые значения b1-b2
            //------------------------------------------------------------------------------------------------------
            Value v1 = new Value(c1, 4.0, 10.0);
            v1.setBoilers(List.of(b1));

            Value v40 = new Value(c1, 9.6, 24.0);
            v40.setBoilers(List.of(b2));

            Value v2 = new Value(c32, "Принудительный вентилятором (закрытая камера сгорания)");
            v2.setBoilers(List.of(b1));

            Value v3 = new Value(c2, 3.3, 9.2);
            v3.setBoilers(List.of(b1));

            Value v4 = new Value(c3, 93.0);
            v4.setBoilers(List.of(b1));

            Value v5 = new Value(c4, 1.6);
            v5.setBoilers(List.of(b1));

            Value v6 = new Value(c5, 1.0);
            v6.setBoilers(List.of(b1));

            Value v7 = new Value(c6, 6.0);
            v7.setBoilers(List.of(b1));

            Value v8 = new Value(c7, 0.5, 3.0);
            v8.setBoilers(List.of(b1));

            Value v9 = new Value(c8, 30.0, 80.0);
            v9.setBoilers(List.of(b1));

            Value v10 = new Value(c9, 35.0, 60.0);
            v10.setBoilers(List.of(b1));

            Value v11 = new Value(c10, 8.0);
            v11.setBoilers(List.of(b1));

            Value v12 = new Value(c11, 3.0, 4.0);
            v12.setBoilers(List.of(b1));

            Value v13 = new Value(c12, 3.0, 4.0);
            v13.setBoilers(List.of(b1));

            Value v14 = new Value(c13, 3.0, 4.0);
            v14.setBoilers(List.of(b1));

            Value v15 = new Value(c14, 220.0, 50.0);
            v15.setBoilers(List.of(b1));

            Value v16 = new Value(c15, 120.0);
            v16.setBoilers(List.of(b1));

            Value v17 = new Value(c16, 60.0, 100.0);
            v17.setBoilers(List.of(b1));

            Value v18 = new Value(c17, "I класс/IPX4D");
            v18.setBoilers(List.of(b1));

            Value v19 = new Value(c18, 28.9);
            v19.setBoilers(List.of(b1));

            Value v39 = new Value(c19, "700х400х299");
            v39.setBoilers(List.of(b1));

            Value v20 = new Value(c32, "Принудительный вентилятором (закрытая камера сгорания)");
            v20.setBoilers(List.of(b2));

            Value v21 = new Value(c2, 8.1, 23.5);
            v21.setBoilers(List.of(b2));

            Value v22 = new Value(c3, 93.0);
            v22.setBoilers(List.of(b2));

            Value v23 = new Value(c4, 2.61);
            v23.setBoilers(List.of(b2));

            Value v24 = new Value(c5, 1.0);
            v24.setBoilers(List.of(b2));

            Value v25 = new Value(c6, 6.0);
            v25.setBoilers(List.of(b2));

            Value v26 = new Value(c7, 0.5, 3.0);
            v26.setBoilers(List.of(b2));

            Value v27 = new Value(c8, 30.0, 80.0);
            v27.setBoilers(List.of(b2));

            Value v28 = new Value(c9, 35.0, 60.0);
            v28.setBoilers(List.of(b2));

            Value v29 = new Value(c10, 8.0);
            v29.setBoilers(List.of(b2));

            Value v30 = new Value(c11, 3.0, 4.0);
            v30.setBoilers(List.of(b2));

            Value v31 = new Value(c12, 3.0, 4.0);
            v31.setBoilers(List.of(b2));

            Value v32 = new Value(c13, 1.0, 2.0);
            v32.setBoilers(List.of(b2));

            Value v33 = new Value(c14, 220.0, 50.0);
            v33.setBoilers(List.of(b2));

            Value v34 = new Value(c15, 120.0);
            v34.setBoilers(List.of(b2));

            Value v35 = new Value(c16, 60.0, 100.0);
            v35.setBoilers(List.of(b2));

            Value v36 = new Value(c17, "I класс/IPX4D");
            v36.setBoilers(List.of(b2));

            Value v37 = new Value(c18, 30.4);
            v37.setBoilers(List.of(b2));

            Value v38 = new Value(c19, "700х400х299");
            v38.setBoilers(List.of(b2));

            acceptableValueService.addAll(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18,
                    v19, v20, v21, v22, v23, v24, v25, v26, v27, v28, v29, v30, v31, v32, v33, v34, v35, v36, v37, v38, v39, v40));

            // Допустимые значения b3-b4
            //------------------------------------------------------------------------------------------------------

            Value v41 = new Value(c1, 4.0, 10.0);
            v41.setBoilers(List.of(b3));

            Value v42 = new Value(c1, 9.6, 24.0);
            v42.setBoilers(List.of(b4));

            Value v43 = new Value(c32, "Принудительный вентилятором (закрытая камера сгорания)");
            v43.setBoilers(List.of(b3));

            Value v44 = new Value(c2, 3.3, 9.2);
            v44.setBoilers(List.of(b3));

            Value v45 = new Value(c3, 93.0);
            v45.setBoilers(List.of(b3));

            Value v46 = new Value(c4, 1.6);
            v46.setBoilers(List.of(b3));

            Value v47 = new Value(c5, 1.0);
            v47.setBoilers(List.of(b3));

            Value v48 = new Value(c6, 6.0);
            v48.setBoilers(List.of(b3));

            Value v49 = new Value(c7, 0.5, 3.0);
            v49.setBoilers(List.of(b3));

            Value v50 = new Value(c8, 30.0, 80.0);
            v50.setBoilers(List.of(b3));

            Value v51 = new Value(c9, 35.0, 60.0);
            v51.setBoilers(List.of(b3));

            Value v52 = new Value(c10, 8.0);
            v52.setBoilers(List.of(b3));

            Value v53 = new Value(c11, 3.0, 4.0);
            v53.setBoilers(List.of(b3));

            Value v54 = new Value(c12, 3.0, 4.0);
            v54.setBoilers(List.of(b3));

            Value v55 = new Value(c13, 3.0, 4.0);
            v55.setBoilers(List.of(b3));

            Value v56 = new Value(c14, 220.0, 50.0);
            v56.setBoilers(List.of(b3));

            Value v57 = new Value(c15, 120.0);
            v57.setBoilers(List.of(b3));

            Value v58 = new Value(c16, 60.0, 100.0);
            v58.setBoilers(List.of(b3));

            Value v59 = new Value(c17, "I класс/IPX4D");
            v59.setBoilers(List.of(b3));

            Value v60 = new Value(c18, 28.9);
            v60.setBoilers(List.of(b3));

            Value v61 = new Value(c19, "700х400х299");
            v61.setBoilers(List.of(b3));

            Value v62 = new Value(c32, "Принудительный вентилятором (закрытая камера сгорания)");
            v62.setBoilers(List.of(b4));

            Value v63 = new Value(c2, 8.1, 23.5);
            v63.setBoilers(List.of(b4));

            Value v64 = new Value(c3, 93.0);
            v64.setBoilers(List.of(b4));

            Value v65 = new Value(c4, 2.61);
            v65.setBoilers(List.of(b4));

            Value v66 = new Value(c5, 1.0);
            v66.setBoilers(List.of(b4));

            Value v67 = new Value(c6, 6.0);
            v67.setBoilers(List.of(b4));

            Value v68 = new Value(c7, 0.5, 3.0);
            v68.setBoilers(List.of(b4));

            Value v69 = new Value(c8, 30.0, 80.0);
            v69.setBoilers(List.of(b4));

            Value v70 = new Value(c9, 35.0, 60.0);
            v70.setBoilers(List.of(b4));

            Value v71 = new Value(c10, 8.0);
            v71.setBoilers(List.of(b4));

            Value v72 = new Value(c11, 3.0, 4.0);
            v72.setBoilers(List.of(b4));

            Value v73 = new Value(c12, 3.0, 4.0);
            v73.setBoilers(List.of(b4));

            Value v74 = new Value(c13, 1.0, 2.0);
            v74.setBoilers(List.of(b4));

            Value v75 = new Value(c14, 220.0, 50.0);
            v75.setBoilers(List.of(b4));

            Value v76 = new Value(c15, 120.0);
            v76.setBoilers(List.of(b4));

            Value v77 = new Value(c16, 60.0, 100.0);
            v77.setBoilers(List.of(b4));

            Value v78 = new Value(c17, "I класс/IPX4D");
            v78.setBoilers(List.of(b4));

            Value v79 = new Value(c18, 30.4);
            v79.setBoilers(List.of(b4));

            Value v80 = new Value(c19, "700х400х299");
            v80.setBoilers(List.of(b4));

            acceptableValueService.addAll(List.of(v41, v42, v43, v44, v45, v46, v47, v48, v49, v50, v51, v52, v53, v54, v55, v56, v57, v58, v59, v60,
                    v61, v62, v63, v64, v65, v66, v67, v68, v69, v70, v71, v72, v73, v74, v75, v76, v77, v78, v79, v80));

            // Допустимые значения b5-b6
            //------------------------------------------------------------------------------------------------------

            Value v81 = new Value(c1, 4.0, 10.0);
            v81.setBoilers(List.of(b5));

            Value v82 = new Value(c56, 7.3, 18.0);
            v82.setBoilers(List.of(b5));

            Value v83 = new Value(c32, "Принудительный вентилятором (закрытая камера сгорания)");
            v83.setBoilers(List.of(b5));

            Value v84 = new Value(c2, 3.3, 9.2);
            v84.setBoilers(List.of(b5));

            Value v85 = new Value(c3, 93.0);
            v85.setBoilers(List.of(b5));

            Value v86 = new Value(c4, 1.6);
            v86.setBoilers(List.of(b5));

            Value v87 = new Value(c5, 1.0);
            v87.setBoilers(List.of(b5));

            Value v88 = new Value(c6, 6.0);
            v88.setBoilers(List.of(b5));

            Value v89 = new Value(c7, 0.5, 3.0);
            v89.setBoilers(List.of(b5));

            Value v90 = new Value(c8, 30.0, 80.0);
            v90.setBoilers(List.of(b5));

            Value v91 = new Value(c58, 35.0, 60.0);
            v91.setBoilers(List.of(b5));

            Value v92 = new Value(c59, 10.0);
            v92.setBoilers(List.of(b5));

            Value v93 = new Value(c60, 8.3);
            v93.setBoilers(List.of(b5));

            Value v94 = new Value(c61, 2.5);
            v94.setBoilers(List.of(b5));

            Value v95 = new Value(c57, 0.2, 8.0);
            v95.setBoilers(List.of(b5));

            Value v96 = new Value(c62, 3.0, 4.0);
            v96.setBoilers(List.of(b5));

            Value v97 = new Value(c12, 3.0, 4.0);
            v97.setBoilers(List.of(b5));

            Value v98 = new Value(c63, 1.0, 2.0);
            v98.setBoilers(List.of(b5));

            Value v99 = new Value(c14, 50.0, 220.0);
            v99.setBoilers(List.of(b5));

            Value v100 = new Value(c15, 120.0);
            v100.setBoilers(List.of(b5));

            Value v101 = new Value(c16, "60x100");
            v101.setBoilers(List.of(b5));

            Value v102 = new Value(c17, "I класс/IPX4D");
            v102.setBoilers(List.of(b5));

            Value v103 = new Value(c18, 28.9);
            v103.setBoilers(List.of(b5));

            Value v104 = new Value(c19, "700x400x299");
            v104.setBoilers(List.of(b5));

            Value v105 = new Value(c1, 9.6, 24.0);
            v105.setBoilers(List.of(b6));

            Value v106 = new Value(c56, 9.6, 24.0);
            v106.setBoilers(List.of(b6));

            Value v107 = new Value(c32, "Принудительный вентилятором (закрытая камера сгорания)");
            v107.setBoilers(List.of(b6));

            Value v108 = new Value(c2, 23.5, 8.1);
            v108.setBoilers(List.of(b6));

            Value v109 = new Value(c3, 93.0);
            v109.setBoilers(List.of(b6));

            Value v110 = new Value(c4, 2.61);
            v110.setBoilers(List.of(b6));

            Value v111 = new Value(c5, 1.0);
            v111.setBoilers(List.of(b6));

            Value v112 = new Value(c6, 6.0);
            v112.setBoilers(List.of(b6));

            Value v113 = new Value(c7, 0.5, 3.0);
            v113.setBoilers(List.of(b6));

            Value v114 = new Value(c8, 30.0, 80.0);
            v114.setBoilers(List.of(b6));

            Value v115 = new Value(c58, 35.0, 60.0);
            v115.setBoilers(List.of(b6));

            Value v116 = new Value(c59, 14.7);
            v116.setBoilers(List.of(b6));

            Value v117 = new Value(c60, 13.3);
            v117.setBoilers(List.of(b6));

            Value v118 = new Value(c61, 2.5);
            v118.setBoilers(List.of(b6));

            Value v119 = new Value(c57, 0.2, 8.0);
            v119.setBoilers(List.of(b6));

            Value v120 = new Value(c62, 3.0, 4.0);
            v120.setBoilers(List.of(b6));

            Value v121 = new Value(c12, 3.0, 4.0);
            v121.setBoilers(List.of(b6));

            Value v122 = new Value(c63, 1.0, 2.0);
            v122.setBoilers(List.of(b6));

            Value v123 = new Value(c14, 50.0, 220.0);
            v123.setBoilers(List.of(b6));

            Value v124 = new Value(c15, 120.0);
            v124.setBoilers(List.of(b6));

            Value v125 = new Value(c16, "60x100");
            v125.setBoilers(List.of(b6));

            Value v126 = new Value(c17, "I класс/IPX4D");
            v126.setBoilers(List.of(b6));

            Value v127 = new Value(c18, 30.4);
            v127.setBoilers(List.of(b6));

            Value v128 = new Value(c19, "700x400x299");
            v128.setBoilers(List.of(b6));

            acceptableValueService.addAll(List.of(v81, v82, v83, v84, v85, v86, v87, v88, v89, v90, v91, v92, v93, v94, v95, v96, v97, v98, v99, v100,
                    v101, v102, v103, v104, v105, v106, v107, v108, v109, v110, v111, v112, v113, v114, v115, v116, v117, v118, v119, v120,
                    v121, v122, v123, v124, v125, v126, v127, v128));

            // Допустимые значения b7-b8
            //------------------------------------------------------------------------------------------------------

            Value v129 = new Value(c20, 20.0);
            v129.setBoilers(List.of(b7));

            Value v130 = new Value(c21, 17.85);
            v130.setBoilers(List.of(b7));

            Value v131 = new Value(c3, 88.0);
            v131.setBoilers(List.of(b7));

            Value v132 = new Value(c71, 1274.0, 1960.0);
            v132.setBoilers(List.of(b7));

            Value v133 = new Value(c72, 2940.0);
            v133.setBoilers(List.of(b7));

            Value v134 = new Value(c73, 2.11);
            v134.setBoilers(List.of(b7));

            Value v135 = new Value(c74, 1.57);
            v135.setBoilers(List.of(b7));

            Value v136 = new Value(c24, 0.025, 0.08);
            v136.setBoilers(List.of(b7));

            Value v137 = new Value(c25, 10.0);
            v137.setBoilers(List.of(b7));

            Value v138 = new Value(c26, "LR20 (2шт.), 3B");
            v138.setBoilers(List.of(b7));

            Value v139 = new Value(c75, "G 1/2 B");
            v139.setBoilers(List.of(b7));

            Value v140 = new Value(c76, "G 1/2 B");
            v140.setBoilers(List.of(b7));

            Value v141 = new Value(c77, "G 1/2 B");
            v141.setBoilers(List.of(b7));

            Value v142 = new Value(c19, "550*330*158");
            v142.setBoilers(List.of(b7));

            Value v143 = new Value(c30, 115.0);
            v143.setBoilers(List.of(b7));

            Value v144 = new Value(c20, 24.0);
            v144.setBoilers(List.of(b8));

            Value v145 = new Value(c21, 21.12);
            v145.setBoilers(List.of(b8));

            Value v146 = new Value(c3, 88.0);
            v146.setBoilers(List.of(b8));

            Value v147 = new Value(c71, 1274.0, 1960.0);
            v147.setBoilers(List.of(b8));

            Value v148 = new Value(c72, 2940.0);
            v148.setBoilers(List.of(b8));

            Value v149 = new Value(c73, 2.59);
            v149.setBoilers(List.of(b8));

            Value v150 = new Value(c74, 1.89);
            v150.setBoilers(List.of(b8));

            Value v151 = new Value(c24, 0.025, 0.08);
            v151.setBoilers(List.of(b8));

            Value v152 = new Value(c25, 12.0);
            v152.setBoilers(List.of(b8));

            Value v153 = new Value(c26, "LR20 (2шт.), 3B");
            v153.setBoilers(List.of(b8));

            Value v154 = new Value(c75, "G 1/2 B");
            v154.setBoilers(List.of(b8));

            Value v155 = new Value(c76, "G 1/2 B");
            v155.setBoilers(List.of(b8));

            Value v156 = new Value(c77, "G 1/2 B");
            v156.setBoilers(List.of(b8));

            Value v157 = new Value(c19, "610*350*161");
            v157.setBoilers(List.of(b8));

            Value v158 = new Value(c30, 108.0);
            v158.setBoilers(List.of(b8));


            acceptableValueService.addAll(List.of(v129, v130, v131, v132, v133, v134, v135, v136, v137, v138, v139, v140,
                    v141, v142, v143, v144, v145, v146, v147, v148, v149, v150, v151, v152, v153, v154, v155, v156, v157, v158));

            // Допустимые значения b9-b10
            //------------------------------------------------------------------------------------------------------

            Value v159 = new Value(c20, 24.0);
            v159.setBoilers(List.of(b9));

            Value v160 = new Value(c21, 25.1);
            v160.setBoilers(List.of(b9));

            Value v161 = new Value(c3, 88.0);
            v161.setBoilers(List.of(b9));

            Value v162 = new Value(c71, 1274.0, 1960.0);
            v162.setBoilers(List.of(b9));

            Value v163 = new Value(c72, 2940.0);
            v163.setBoilers(List.of(b9));

            Value v164 = new Value(c73, 2.11);
            v164.setBoilers(List.of(b9));

            Value v165 = new Value(c74, 1.57);
            v165.setBoilers(List.of(b9));

            Value v166 = new Value(c24, 0.015, 0.08);
            v166.setBoilers(List.of(b9));

            Value v167 = new Value(c25, 10.0);
            v167.setBoilers(List.of(b9));

            Value v168 = new Value(c26, "LR20 (2шт.), 3B");
            v168.setBoilers(List.of(b9));

            Value v169 = new Value(c75, "G 1/2 B");
            v169.setBoilers(List.of(b9));

            Value v170 = new Value(c76, "G 1/2 B");
            v170.setBoilers(List.of(b9));

            Value v171 = new Value(c77, "G 1/2 B");
            v171.setBoilers(List.of(b9));

            Value v172 = new Value(c19, "550*330*158");
            v172.setBoilers(List.of(b9));

            Value v173 = new Value(c30, 130.0);
            v173.setBoilers(List.of(b9));

            Value v174 = new Value(c20, 24.0);
            v174.setBoilers(List.of(b10));

            Value v175 = new Value(c21, 21.12);
            v175.setBoilers(List.of(b10));

            Value v176 = new Value(c3, 88.0);
            v176.setBoilers(List.of(b10));

            Value v177 = new Value(c71, 1274.0, 1960.0);
            v177.setBoilers(List.of(b10));

            Value v178 = new Value(c72, 2940.0);
            v178.setBoilers(List.of(b10));

            Value v179 = new Value(c73, 2.59);
            v179.setBoilers(List.of(b10));

            Value v180 = new Value(c74, 1.89);
            v180.setBoilers(List.of(b10));

            Value v181 = new Value(c24, 0.015, 0.08);
            v181.setBoilers(List.of(b10));

            Value v182 = new Value(c25, 12.0);
            v182.setBoilers(List.of(b10));

            Value v183 = new Value(c26, "LR20 (2шт.), 3B");
            v183.setBoilers(List.of(b10));

            Value v184 = new Value(c75, "G 1/2 B");
            v184.setBoilers(List.of(b10));

            Value v185 = new Value(c76, "G 1/2 B");
            v185.setBoilers(List.of(b10));

            Value v186 = new Value(c77, "G 1/2 B");
            v186.setBoilers(List.of(b10));

            Value v187 = new Value(c19, "610*350*161");
            v187.setBoilers(List.of(b10));

            Value v188 = new Value(c30, " ");
            v188.setBoilers(List.of(b10));

            acceptableValueService.addAll(List.of(v159, v160, v161, v162, v163, v164, v165, v166, v167, v168, v169, v170, v171, v172, v173, v174, v175, v176, v177, v178,
                    v179, v180, v181, v182, v183, v184, v185, v186, v187, v188));

            // Допустимые значения b11-b12
            //------------------------------------------------------------------------------------------------------
            Value v189 = new Value(c31, "открытая");
            v189.setBoilers(List.of(b11));

            Value v190 = new Value(c32, "Принудительно вентилятором");
            v190.setBoilers(List.of(b11));

            Value v191 = new Value(c33, "да");
            v191.setBoilers(List.of(b11));

            Value v192 = new Value(c20, 26.0);
            v192.setBoilers(List.of(b11));

            Value v193 = new Value(c3, 89.0);
            v193.setBoilers(List.of(b11));

            Value v194 = new Value(c71, 1274.0, 1960.0);
            v194.setBoilers(List.of(b11));

            Value v195 = new Value(c72, 2940.0);
            v195.setBoilers(List.of(b11));

            Value v196 = new Value(c73, 2.79);
            v196.setBoilers(List.of(b11));

            Value v197 = new Value(c74, 1.04);
            v197.setBoilers(List.of(b11));

            Value v198 = new Value(c24, 0.015, 0.08);
            v198.setBoilers(List.of(b11));

            Value v199 = new Value(c35, 2.5);
            v199.setBoilers(List.of(b11));

            Value v200 = new Value(c25, 13.0);
            v200.setBoilers(List.of(b11));

            Value v201 = new Value(c36, 50.0, 220.0);
            v201.setBoilers(List.of(b11));

            Value v202 = new Value(c37, "Автоматическое электронное");
            v202.setBoilers(List.of(b11));

            Value v203 = new Value(c75, "G 1/2 B");
            v203.setBoilers(List.of(b11));

            Value v204 = new Value(c76, "G 1/2 B");
            v204.setBoilers(List.of(b11));

            Value v205 = new Value(c77, "G 1/2 B");
            v205.setBoilers(List.of(b11));

            Value v206 = new Value(c31, "открытая");
            v206.setBoilers(List.of(b12));

            Value v207 = new Value(c32, "Принудительно вентилятором");
            v207.setBoilers(List.of(b12));

            Value v208 = new Value(c33, "да");
            v208.setBoilers(List.of(b12));

            Value v209 = new Value(c20, 32.0);
            v209.setBoilers(List.of(b12));

            Value v210 = new Value(c3, 89.0);
            v210.setBoilers(List.of(b12));

            Value v211 = new Value(c71, 1274.0, 1960.0);
            v211.setBoilers(List.of(b12));

            Value v212 = new Value(c72, 2940.0);
            v212.setBoilers(List.of(b12));

            Value v213 = new Value(c73, 3.45);
            v213.setBoilers(List.of(b12));

            Value v214 = new Value(c74, 2.52);
            v214.setBoilers(List.of(b12));

            Value v215 = new Value(c24, 0.015, 0.08);
            v215.setBoilers(List.of(b12));

            Value v216 = new Value(c35, 2.5);
            v216.setBoilers(List.of(b12));

            Value v217 = new Value(c25, 16.0);
            v217.setBoilers(List.of(b12));

            Value v218 = new Value(c36, 50.0, 220.0);
            v218.setBoilers(List.of(b12));

            Value v219 = new Value(c37, "Автоматическое электронное");
            v219.setBoilers(List.of(b12));

            Value v220 = new Value(c75, "G 1/2 B");
            v220.setBoilers(List.of(b12));

            Value v221 = new Value(c76, "G 1/2 B");
            v221.setBoilers(List.of(b12));

            Value v222 = new Value(c77, "G 1/2 B");
            v222.setBoilers(List.of(b12));

            acceptableValueService.addAll(List.of(v189, v190, v191, v192, v193, v194, v195, v196, v197, v198, v199,
                    v200, v201, v202, v203, v204, v205, v206, v207, v208, v209, v210, v211, v212, v213, v214, v215,
                    v216, v217, v218, v219, v220, v221, v222));

            // Допустимые значения b28-b29
            //----------------------------------------------------------------------------------------------------------
            Value v223 = new Value(c64, 200.0);
            v223.setBoilers(List.of(b28));
            Value v224 = new Value(c64, 300.0);
            v224.setBoilers(List.of(b29));

            Value v225 = new Value(c19, 510.0, 1645.0);
            v225.setBoilers(List.of(b28));
            Value v226 = new Value(c19, 510.0, 1722.0);
            v226.setBoilers(List.of(b29));

            Value v227 = new Value(c65, 58.5, 66.2);
            v227.setBoilers(List.of(b28));
            Value v228 = new Value(c65, 81.5, 90.5);
            v228.setBoilers(List.of(b29));

            Value v229 = new Value(c70, 280.0);
            v229.setBoilers(List.of(b28));
            Value v230 = new Value(c70, 420.0);
            v230.setBoilers(List.of(b29));

            Value v233 = new Value(c68, 3.0);
            v233.setBoilers(List.of(b28, b29));

            // Допустимые значения b20-b29
            //----------------------------------------------------------------------------------------------------------
            Value v234 = new Value(c69, "IPX4");
            v234.setBoilers(List.of(b20, b21, b22, b23, b24, b25, b26, b27, b28, b29));

            Value v232 = new Value(c67, "220V-240V, 50HZ");
            v232.setBoilers(List.of(b18, b19, b20, b21, b22, b23, b24, b25, b26, b27, b28, b29));

            Value v231 = new Value(c66, 0.8);
            v231.setBoilers(List.of(b18, b19, b20, b21, b22, b23, b24, b25, b26, b27, b28, b29));

            // Допустимые значения b24-b27
            //----------------------------------------------------------------------------------------------------------
            Value v235 = new Value(c68, 2.0);
            v235.setBoilers(List.of(b24, b25, b26, b27));

            Value v242 = new Value(c70, 47.0);
            v242.setBoilers(List.of(b20, b22, b24, b26));
            Value v243 = new Value(c70, 126.0);
            v243.setBoilers(List.of(b25, b27));

            Value v240 = new Value(c65, 17.2, 20.2);
            v240.setBoilers(List.of(b24, b26));
            Value v241 = new Value(c65, 30.2, 33.4);
            v241.setBoilers(List.of(b25, b27));

            Value v238 = new Value(c19, "Ш 458 В 635 Г 248");
            v238.setBoilers(List.of(b24, b26));
            Value v239 = new Value(c19, "Ш 563 В 940 Г 297");
            v239.setBoilers(List.of(b25, b27));

            Value v236 = new Value(c64, 30.0);
            v236.setBoilers(List.of(b20, b22, b24, b26));
            Value v237 = new Value(c64, 80.0);
            v237.setBoilers(List.of(b25, b27));

            // Допустимые значения b18-b24
            //----------------------------------------------------------------------------------------------------------
            Value v244 = new Value(c68, 1.5);
            v244.setBoilers(List.of(b18, b19, b20, b21, b22, b23));

            // Допустимые значения b22-b24
            //----------------------------------------------------------------------------------------------------------
            Value v249 = new Value(c70, 156.0);
            v249.setBoilers(List.of(b21, b23));

            Value v250 = new Value(c65, 13.0, 15.2);
            v250.setBoilers(List.of(b22));
            Value v251 = new Value(c65, 29.0, 33.2);
            v251.setBoilers(List.of(b23));

            Value v252 = new Value(c19, 390.0, 520.0);
            v252.setBoilers(List.of(b28));
            Value v253 = new Value(c19, 460.0, 940.0);
            v253.setBoilers(List.of(b29));

            Value v254 = new Value(c64, 100.0);
            v254.setBoilers(List.of(b21, b23));

            // Допустимые значения b20-b21
            //----------------------------------------------------------------------------------------------------------
            Value v255 = new Value(c65, 12.8, 15.1);
            v255.setBoilers(List.of(b20));
            Value v256 = new Value(c65, 26.8, 30.5);
            v256.setBoilers(List.of(b21));

            Value v257 = new Value(c19, 340.0, 624.0);
            v257.setBoilers(List.of(b20));
            Value v258 = new Value(c19, 410.0, 1106.0);
            v258.setBoilers(List.of(b21));

            // Допустимые значения b18-b19
            //----------------------------------------------------------------------------------------------------------
            Value v259 = new Value(c79, "Верхнее");
            v259.setBoilers(List.of(b18, b19));

            Value v260 = new Value(c70, 21.0);
            v260.setBoilers(List.of(b18));
            Value v261 = new Value(c70, 32.0);
            v261.setBoilers(List.of(b19));

            Value v262 = new Value(c65, 10.0, 10.5);
            v262.setBoilers(List.of(b18));
            Value v263 = new Value(c65, 11.0, 12.2);
            v263.setBoilers(List.of(b19));

            Value v264 = new Value(c19, 280.0, 360.0);
            v264.setBoilers(List.of(b18));
            Value v265 = new Value(c19, 280.0, 475.0);
            v265.setBoilers(List.of(b19));

            Value v266 = new Value(c64, 10.0);
            v266.setBoilers(List.of(b18));
            Value v267 = new Value(c64, 15.0);
            v267.setBoilers(List.of(b19));

            acceptableValueService.addAll(List.of(v223, v224, v225, v226, v227, v228, v229, v230, v231, v232, v233, v234,
                    v235, v236, v237, v238, v239, v240, v241, v242, v243, v244, v249, v250, v251, v252, v253, v254, v255,
                    v256, v257, v258, v259, v260, v261, v262, v263, v264, v265, v266
            ));

        }
    }
}
