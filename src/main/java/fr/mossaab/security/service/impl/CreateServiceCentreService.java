package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.ServiceCentre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreateServiceCentreService {
    @Autowired
    private ServiceCentreService serviceCentreService;

    public void CreateServiceCentre(){
        ServiceCentre serviceCentre1 = new ServiceCentre("ООО \"Гудмаер\"", "Ижевск", "с. Завьялово, ул. Гольянская, д. 2а", "(3412)77-11-04 доб. 2", "Газовые котлы, колонки");

        ServiceCentre serviceCentre2 = new ServiceCentre("ГК «Термотехника»", "Чебоксары", "г. Чебоксары, Петрова С.П., д. 6, строение 2", "(8352) 57-32-44, 57-34-44", "Газовые котлы, колонки");

        ServiceCentre serviceCentre3 = new ServiceCentre("ООО «Тепломеханика Газ»", "Кемерово", "г. Кемерово, улица Сарыгина, дом 27, офис 103", "(3842) 67-02-88", "Газовые котлы");

        ServiceCentre serviceCentre4 = new ServiceCentre("ООО «ЦИК Аквавольт»", "Новосибирск", "Новосибирская область, г. Бердск, ул. Зеленая Роща, дом 7/34, этаж 1", "(7913) 484-1010", "Газовые котлы");

        ServiceCentre serviceCentre5 = new ServiceCentre("ООО \"Газмастер\"", "Белгород", "г. Белгород, ул. Гостенская, д. 12", "+7 (903) 642-80-53", "Котлы, колонки");

        ServiceCentre serviceCentre6 = new ServiceCentre("ИП Ливцов В.В.", "Саранск", "г. Саранск, ул. Рабочая, д.№169", "(8342)- 34-50-30", "Газовые котлы, колонки");

        ServiceCentre serviceCentre7 = new ServiceCentre("ООО \"Котельный сервис\"", "Оренбург", "г. Оренбург, ул. Карагандинская, д. 32", "(3532) 92-70-00", "Газовые котлы, колонки");

        ServiceCentre serviceCentre8 = new ServiceCentre("ИП Фоменко А.А.", "Ставрополь", "г. Ставрополь 3-я Промышленная дом 3 ТЦ «Мой Дом»", "(8652) 92-00-32", "Газовые котлы, колонки");

        ServiceCentre serviceCentre9 = new ServiceCentre("ООО Компания «СЭТ»", "Ставрополь", "г. Ставрополь, ул. Тухачевского, д. 22/4", "+7(928) 321-85-83", "Газовые котлы");

        ServiceCentre serviceCentre10 = new ServiceCentre("ООО «Гео-Газ-Сервис»", "Георгиевск", "Ставропольский, город Георгиевск, улица Воровского, 1", "(87951) 3-66-80", "Газовые котлы");

        ServiceCentre serviceCentre11 = new ServiceCentre("ООО «Аксон»", "Екатеринбург", "г.Екатеринбург, ул Московская, стр. 287, помещение 32", "(343)345-22-13", "Газовые котлы, колонки");

        ServiceCentre serviceCentre12 = new ServiceCentre("ООО «СЗГАЗ»", "Санкт-Петербург", "г.Санкт-Петербург, Ленинский проспект 51-507", "(812)244-68-82", "Газовые котлы, колонки");

        ServiceCentre serviceCentre13 = new ServiceCentre("ИП Зиновьев Д.И.", "Брянск", "г. Брянск, c.Супонево, ул. Московская, д. 517", "(4832) 92-23-21", "Газовые котлы, колонки");

        ServiceCentre serviceCentre14 = new ServiceCentre("ООО \"Теплоцентр\"", "Казань", "г. Казань, Проспект Победы, д. 90", "(843) 250-40-60, (843) 266-55-06", "Газовые котлы, колонки");

        ServiceCentre serviceCentre15 = new ServiceCentre("ООО \"Регионторг\"", "Курган", "г. Курган пр. Машиностроителей, 30", "(3522) 601-701 доб. 101", "Газовые котлы, колонки");

        ServiceCentre serviceCentre16 = new ServiceCentre("ООО «Газсервис»", "Челябинск", "Челябинская область, г.Коркино, ул. Цвилинга д. 28, оф.28", "", "Газовые котлы, колонки");

        ServiceCentre serviceCentre17 = new ServiceCentre("ИП Усачев", "Воронеж", "г.Воронеж, ул.Дорожная, д.2", "+7 920-40-88-444", "Газовые котлы, колонки");

        ServiceCentre serviceCentre18 = new ServiceCentre("ИП Валиуллин Р.Р.", "Белгород", "г. Белгород ул. Победы 69а", "+7 906-608-0204", "Газовые котлы, колонки");

        ServiceCentre serviceCentre19 = new ServiceCentre("ООО «Стройкомплект»", "Самара", "г. Самара, ул. Молодогвардейская, д.104, оф.6", "(846) 332-14-34", "Газовые котлы, колонки");

        ServiceCentre serviceCentre20 = new ServiceCentre("ООО \"ЛенГазСервис\"", "Санкт-Петербург", "Санкт-Петербург, ул. Автовская, 16, Литер А, кабинет 116", "+7 911-922-44-58", "Газовые котлы, колонки");

        ServiceCentre serviceCentre21 = new ServiceCentre("ООО \"Аванпост\"", "Владимир", "г.Владимир, ул.Б.Нижегородская, 1-а", "(4922) 32-22-10", "Газовые котлы, колонки");

        ServiceCentre serviceCentre22 = new ServiceCentre("ООО «СОКОЛ»", "Орел", "Орел, ул. Гагарина, д.23", "+7 920-829-32-23", "Газовые котлы, колонки");

        ServiceCentre serviceCentre23 = new ServiceCentre("ИП Киселев А.В.", "Нальчик", "г. Нальчик, ул. Мальбахова, д. 35", "+7 928-711-40-72", "Газовые котлы, колонки");

        ServiceCentre serviceCentre24 = new ServiceCentre("ООО \"Компания Оптима\"", "Тула", "г.Тула, ул. Октябрьская, д.80А. пом/оф/лит I/1/А эт 1", "(4872) 25-20-89", "Газовые котлы, колонки");

        ServiceCentre serviceCentre25 = new ServiceCentre("ООО «Мир тепла сервис»", "Н.Новгород", "г. Н.Новгород, п. Черепичный, д.14, офис 10", "(831) 413-35-15", "Газовые котлы, колонки");

        ServiceCentre serviceCentre26 = new ServiceCentre("ИП Воробьев Т.В.", "Омск", "г. Омск, ул. 8 Амурская, д. 64/1", "+7 913-967-80-17", "Газовые котлы, колонки");

        ServiceCentre serviceCentre27 = new ServiceCentre("ООО \"Газовик Сервис\"", "Пенза", "Пенза г, Чаадаева ул, стр 135", "(4812) 26-29-27", "Газовые котлы, колонки");

        ServiceCentre serviceCentre28 = new ServiceCentre("ООО «РЕСУРС-МРГ»", "Кострома", "ул. Костромская д. 105 неж. пом. №5", "+7 920-646-19-97", "Газовые котлы, колонки");

        ServiceCentre serviceCentre29 = new ServiceCentre("ООО «ТеплотехникаСервис»", "Тула", "г. Тула, ул. Оборонная, д. 37", "(4872) 700-113", "Газовые котлы, колонки");

        ServiceCentre serviceCentre30 = new ServiceCentre("ИП Лукин", "Липецк", "проезд Сержанта Кувшинова, д. 10, оф. 32", "+7 960-142-88-81", "Газовые котлы, колонки");

        ServiceCentre serviceCentre31 = new ServiceCentre("ООО \"ГОРГАЗ\"", "Новоалтайск", "г. Новоалтайск, ул.Белякова 1а, оф.22", "(38532)56393; 8(3852)600420; 89132100420", "Газовые котлы и колонки");

        ServiceCentre serviceCentre32 = new ServiceCentre("ИП Нуртдинов", "Пермь", "г. Пермь, ул. Карпинского, д.83.", "(342) 280-16-61, (342) 290-38-88", "Газовые котлы и колонки");

        ServiceCentre serviceCentre33 = new ServiceCentre("ИП Вилков", "Н. Челны", "г. Набережные Челны, пр-т Московский, д.89", "+7 937-777-67-99", "Газовые котлы и колонки");

        ServiceCentre serviceCentre34 = new ServiceCentre("ИП Кузнецова", "Орел", "г. Орел, ул. Медведева, д.30", "(4862)48-85-89", "Газовые котлы, электрокотлы, колонки");

        ServiceCentre serviceCentre35 = new ServiceCentre("ИП Лисичкин С.В.", "Шахты", "г.Шахты, ул. Шевченко, д.109", "+7 950-860-58-57, +7 952-588-02-16, +7 918-542-49-39", "Газовые котлы, электрокотлы, колонки");

        ServiceCentre serviceCentre36 = new ServiceCentre("ИП Мартыненков Ю.Л.", "Смоленск", "г. Смоленск, ул. Шевченко д. 83", "+7 920-666-26-63", "Газовые котлы и колонки");

        ServiceCentre serviceCentre37 = new ServiceCentre("ИП Савоськина А.Ю.", "Орел", "г. Орел, Пионерская ул., д. 4", "(4862)63-22-62", "Газовые котлы, электрокотлы, колонки");

        ServiceCentre serviceCentre38 = new ServiceCentre("ИП Комышов Ф.И.", "Козельск", "г. Козельск, ул. Чкалова, д. 40", "+7 920-612-10-77", "Газовые котлы, электрокотлы, колонки");

        ServiceCentre serviceCentre39 = new ServiceCentre("ООО \"ТЕПЛОСИТИ-96\"", "Екатеринбург", "г Екатеринбург, пр-кт Орджоникидзе, строение 8, офис 305", "+7 965-522-47-79", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre40 = new ServiceCentre("ООО «ТД «Феникс»", "Аксай", "Ростовская область, м.р-н Аксайский, с.п. Щепкинское, х. Нижнетемерницкий, ул. Гайдара, зд.6, скл. 3/4, помещ.1", "+7 903-471-64-40", "Газовые котлы и колонки");

        ServiceCentre serviceCentre41 = new ServiceCentre("ООО «ОТОПИТЕЛЬНЫЕ СИСТЕМЫ»", "Новосибирск", "г. Новосибирск ул.9-го Ноября 95", "(383) 380-93-79", "Газовые котлы и колонки");

        ServiceCentre serviceCentre42 = new ServiceCentre("АО «Газпром газораспределение Брянск»", "Брянск", "г. Брянск, ул. Щукина, д. 54", "(4832) 74-31-88", "Газовые котлы и колонки");

        ServiceCentre serviceCentre43 = new ServiceCentre("ТП Калабин Д.И.", "Сосново", "Пермский край, Чайковский район, с.Сосново, ул. Советская, д.81", "+7 922-646-22-99, +7 922-318-63-", "Газовые котлы и колонки");

        ServiceCentre serviceCentre44 = new ServiceCentre("ИП Магомедов М.М.", "Махачкала, Каспийск", "Респ. Дагестан, г. Каспийск, ул. Ермака, д. 13.", "+7 989-899-96-69", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre45 = new ServiceCentre("ИП Утемов А.С.", "Суксун", "Пермский край, Суксунский район, п. Суксун, ул. Маношина, д. 11", "+7 950-454-39-37", "Газовые котлы и колонки");

        ServiceCentre serviceCentre46 = new ServiceCentre("ИП Аксенов К.В", "Медынь", "г. Медынь, пр-т Ленина, д, 27, пом. 9", "+7 920-615-53-44", "Газовые котлы и колонки");

        ServiceCentre serviceCentre47 = new ServiceCentre("ООО «ГорГаз»", "Дзержинск", "г. Дзержинск, пр-т Ленина, д. 105Б, оф. 1", "(8313) 234-666, 233-662, +7 904-049-76-14", "Газовые котлы и колонки");

        ServiceCentre serviceCentre48 = new ServiceCentre("ООО «СаяныЭнергоСервис»", "Н. Новгород", "г. Нижний Новгород пр. Ленина, 93, офис 10", "(831) 22-888-00", "Газовые котлы и колонки");

        ServiceCentre serviceCentre49 = new ServiceCentre("ООО \"МТ Сервис\"", "Н. Новгород", "Н. Новгород, ул. Кемеровская д.12, офис 3", "(831) 413-35-15", "Газовые котлы и колонки");

        ServiceCentre serviceCentre50 = new ServiceCentre("ИП Середа А.Н.", "Пермь", "г. Пермь, ул. Решетникова, д. 17", "(34261) 44368", "Газовые котлы и колонки");

        ServiceCentre serviceCentre51 = new ServiceCentre("ООО \"Теплый Дом\"", "Мичуринск", "г.Мичуринск, Садовый проезд д.12 Б", "(47545) 2-01-11", "Газовые котлы, колонки, ЭВН");

        ServiceCentre serviceCentre52 = new ServiceCentre("ИП Расторгуев В.В.", "Южноуральск", "г. Южноуральск, ул. Московская, д. 24", "+7 963-079-66-66", "Газовые котлы и колонки");

        ServiceCentre serviceCentre53 = new ServiceCentre("ИП Барабанов А.П.", "Н. Новгород", "Нижегородская обл. Кстовский район, дер. Крутая, ул. Березовая д.101", "+7 920-020-75-96", "Газовые котлы");

        ServiceCentre serviceCentre54 = new ServiceCentre("ООО \"ИСЦ\"", "Ульяновск", "г.Ульяновск, ул.Промышленная, д.30", "(9272) 70-16-39", "Газовые котлы и колонки");

        ServiceCentre serviceCentre55 = new ServiceCentre("ООО \"СамараКомплектСервис-М\"", "Самара", "г. Самара,ул. Партизанская,д.80,ком.11", "(846) 202-12-00", "Газовые котлы и колонки");

        ServiceCentre serviceCentre56 = new ServiceCentre("ООО \"Аква-Юг\"", "Краснодар", "г. Краснодар, ул. Российская, д. 253/1", "(861) 228-98-55", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre57 = new ServiceCentre("ООО «Теплоград»", "Коломна", "г.Коломна, ул. Октябрьской революции, д.387 «а»", "8(926)907-53-00", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre58 = new ServiceCentre("ООО \"Евросесвис\"", "Шилово", "Рязанская обл, р.п.Шилово, ул.Советская,д.46, помещение Н5", "+7 910-610-22-44", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre59 = new ServiceCentre("ООО «ЕСМ»", "Ульяновск", "г. Ульяновск, пер. Робеспьера, дом 2/79, офис 10", "(8422)76-52-91", "Газовые котлы");

        ServiceCentre serviceCentre60 = new ServiceCentre("ИП Лукьянов Р.М.", "Симферополь", "г. Симферополь, ул. Кубанская, д. 32", "+7 978-835-23-57", "ЭВН");

        ServiceCentre serviceCentre61 = new ServiceCentre("ООО \"ИНГТЕПЛО\"", "Назрань", "г. Сунжа, ул. Висаитова, д. 51 офис 3", "+7 928-695-62-99", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre62 = new ServiceCentre("ООО \"БРАВО-ГРУПП\"", "", "", "", "");

        ServiceCentre serviceCentre63 = new ServiceCentre("ООО «Академия Мастеров»", "Пермь", "г. Пермь, ул. Переездная 2, литер С", "(342) 288-68-18", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre64 = new ServiceCentre("ООО «ИЦЛТ»", "Новосибирск", "г Новосибирск, ул Мусы Джалиля 23", "+7 913-007-61-79, +7 993-007-06-45", "Газовые котлы, электрокотлы, колонки");

        ServiceCentre serviceCentre65 = new ServiceCentre("ООО «РАКУРС»", "Мичуринск", "г.Мичуринск, ул.Лаврова д.69/10", "+7 910-750-67-27", "Газовые котлы, колонки");

        ServiceCentre serviceCentre66 = new ServiceCentre("ИП Акулов М.А.", "Арзамас", "г. Арзамас, ул. Октябрьская, д. 34А", "+7 908-239-73-94", "Газовые котлы");

        ServiceCentre serviceCentre67 = new ServiceCentre("ГАЗТЕПЛОСЕРВИС", "Курган", "г. Курган, ул. Куйбышева, 56", "(3522)66-25-65", "Газовые котлы");

        ServiceCentre serviceCentre68 = new ServiceCentre("Юсупбаев Э.Ю.", "Подольск", "г Подольск, ул Давыдова, Д 5", "+7 925 925-34-95", "Газовые котлы, электрокотлы, колонки, ЭВН");

        ServiceCentre serviceCentre69 = new ServiceCentre("ООО «ТЕРМ Центр»", "Жуковский", "Московская область, г. Жуковский, ул. Мясищева, д.1, офис 313", "(499) 670-45-46", "Газовые котлы и колонки");

        ServiceCentre serviceCentre70 = new ServiceCentre("ООО «Первый Газовый»", "Калуга", "г. Калуга, ул. Декабристов, д. 15, оф.1", "(4842) 56-34-11; +7(4842)59-53-07", "Газовые котлы, электрокотлы, колонки, ЭВН");

        List<ServiceCentre> serviceCentre = List.of(
                serviceCentre1, serviceCentre2, serviceCentre3, serviceCentre4, serviceCentre5,
                serviceCentre6, serviceCentre7, serviceCentre8, serviceCentre9, serviceCentre10,
                serviceCentre11, serviceCentre12, serviceCentre13, serviceCentre14, serviceCentre15,
                serviceCentre16, serviceCentre17, serviceCentre18, serviceCentre19, serviceCentre20,
                serviceCentre21, serviceCentre22, serviceCentre23, serviceCentre24, serviceCentre25,
                serviceCentre26, serviceCentre27, serviceCentre28, serviceCentre29, serviceCentre30,
                serviceCentre31, serviceCentre32, serviceCentre33, serviceCentre34, serviceCentre35,
                serviceCentre36, serviceCentre37, serviceCentre38, serviceCentre39, serviceCentre40,
                serviceCentre41, serviceCentre42, serviceCentre43, serviceCentre44, serviceCentre45,
                serviceCentre46, serviceCentre47, serviceCentre48, serviceCentre49, serviceCentre50,
                serviceCentre51, serviceCentre52, serviceCentre53, serviceCentre54, serviceCentre55,
                serviceCentre56, serviceCentre57, serviceCentre58, serviceCentre59, serviceCentre60,
                serviceCentre61, serviceCentre62, serviceCentre63, serviceCentre64, serviceCentre65,
                serviceCentre66, serviceCentre67, serviceCentre68, serviceCentre69, serviceCentre70
        );

        serviceCentreService.addServiceCentre(serviceCentre);
    }

}
