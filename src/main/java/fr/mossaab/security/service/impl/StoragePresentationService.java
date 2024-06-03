package fr.mossaab.security.service.impl;

/*@Service
public class StoragePresentationService {
    //Раскомментировать и изменить под свою локальную структуру

    //private final String FOLDER_PATH = "C:/Users/Admin/Desktop/pre/";

    //Закомментировать в случае локального использования
    private final String FOLDER_PATH="/var/www/vuary/";
    @Autowired
    private FileDataPresentationRepository fileDataPresentationRepository;

    public FileDataPresentation uploadImageToSql(Presentation presentation, String number) throws IOException {
        String str = presentation.getTitle();
        str = str.substring(0, str.length() - 1);

        String filePath = FOLDER_PATH + str + "/" + presentation.getTitle() + number + ".png";

        FileDataPresentation fileDataPresentation = fileDataPresentationRepository.save(FileDataPresentation.builder()
                .name(presentation.getTitle() + number + ".png")
                .type("image/jpeg")
                .filePath(filePath).build());
        fileDataPresentationRepository.save(fileDataPresentation);
        return fileDataPresentation;
    }

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<FileDataPresentation> fileData = fileDataPresentationRepository.findByName(fileName);
        String filePath=fileData.get().getFilePath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }
}*/
