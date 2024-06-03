package fr.mossaab.security.service.impl;

import fr.mossaab.security.config.PathConfig;
import fr.mossaab.security.entities.PassportCategory;
import fr.mossaab.security.entities.PassportFileData;
import fr.mossaab.security.entities.PassportTitle;
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
        if (passportCategoryRepository.count() == 0 && passportTitleRepository.count() == 0 && passportFileDataRepository.count() == 0) {
            // Создание категорий
            PassportCategory category1 = new PassportCategory();
            category1.setTitle("Газовые колонки");
            PassportCategory category2 = new PassportCategory();
            category2.setTitle("Газовые котлы");
            PassportCategory category3 = new PassportCategory();
            category3.setTitle("Электрические котлы");
            PassportCategory category4 = new PassportCategory();
            category4.setTitle("Электрические накопительные водонагреватели");
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
        }
    }
}
