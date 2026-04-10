package fr.mossaab.security.validation.validators;


import fr.mossaab.security.validation.annotation.ValidImageFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    private long maxSizeBytes;
    private Set<String> allowedContentTypes;

    @Override
    public void initialize(ValidImageFile constraintAnnotation) {
        this.maxSizeBytes = constraintAnnotation.maxSizeBytes();
        this.allowedContentTypes = new HashSet<>(Arrays.asList(constraintAnnotation.allowedContentTypes()));
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // null — считаем невалидным (файл обязателен)
        if (file == null) {
            return false;
        }

        // пустой файл — невалиден
        if (file.isEmpty()) {
            addMessage(context, "Файл не должен быть пустым");
            return false;
        }

        // проверка размера
        if (file.getSize() > maxSizeBytes) {
            addMessage(context, "Размер файла не должен превышать " + maxSizeBytes + " байт");
            return false;
        }

        // проверка MIME-типа
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            addMessage(context, "Допустимы только файлы JPEG и PNG");
            return false;
        }

        return true;
    }

    private void addMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
