package fr.mossaab.security.service;

import ch.qos.logback.core.joran.sanity.Pair;
import fr.mossaab.security.entities.FileDataPresentation;
import fr.mossaab.security.entities.Presentation;
import fr.mossaab.security.repository.PresentationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class PresentationService {

    @Autowired
    private PresentationRepository presentationRepository;

    @Autowired
    private StoragePresentationService storagePresentationService;

    public void createPresentationWithFiles(String title, int fileCount) throws IOException {
        // Создание презентации
        Presentation presentation = new Presentation();
        presentation.setTitle(title);

        // Сохранение презентации в базе данных
        presentation = presentationRepository.save(presentation);

        // Массив для хранения файлов
        FileDataPresentation[] files = new FileDataPresentation[fileCount];

        // Загрузка файлов к презентации
        for (int i = 0; i < fileCount; i++) {
            files[i] = storagePresentationService.uploadImageToSql(presentation, String.format("%02d", i + 1));
        }

        // Связывание файлов с презентацией
        presentation.setFileDataPresentation(Arrays.asList(files));

        // Сохранение обновленной презентации в базе данных
        presentationRepository.save(presentation);
    }

    // Метод для вызова createPresentationWithFiles() с разными аргументами через цикл
    public void createMultiplePresentations() throws IOException {
        if (presentationRepository.count() == 0) {
            // Создаем список объектов PresentationData с нужными аргументами
            List<PresentationData> presentationsData = new ArrayList<>();
            presentationsData.add(new PresentationData("Passport_VFHF_A5_new-", 12));
            presentationsData.add(new PresentationData("Passport_FRM_A5_3mm_new-", 12));
            presentationsData.add(new PresentationData("Passport_FRM_A5_new-", 12));
            presentationsData.add(new PresentationData("Passport_VFE_A5_new-", 12));
            presentationsData.add(new PresentationData("Passport_Electrokotel_A4_prt-", 40));
            presentationsData.add(new PresentationData("Passport_KM_A5_new-", 12));
            presentationsData.add(new PresentationData("10-24DK-", 55));
            presentationsData.add(new PresentationData("Паспорт_T10-24_OK-", 51));
            presentationsData.add(new PresentationData("Паспорт_колонка_S10-12, S10-12EM-", 29));
            presentationsData.add(new PresentationData("Паспорт_колонка_Suari_Турбо_1-", 26));
            presentationsData.add(new PresentationData("Паспорт_колонка_S10_12_S10_12EM-", 12));
            // Цикл для вызова метода createPresentationWithFiles() с разными аргументами
            for (PresentationData presentationData : presentationsData) {
                createPresentationWithFiles(presentationData.getTitle(), presentationData.getFileCount());
            }
        }
    }
    private static class PresentationData {
        private String title;
        private int fileCount;

        public PresentationData(String title, int fileCount) {
            this.title = title;
            this.fileCount = fileCount;
        }

        public String getTitle() {
            return title;
        }

        public int getFileCount() {
            return fileCount;
        }
    }
}