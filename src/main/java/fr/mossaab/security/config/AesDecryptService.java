package fr.mossaab.security.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AesDecryptService {

    @Value("${aes.secret}")
    private String aesSecret;

    private SecretKeySpec secretKeySpec;

    private static final int GCM_IV_LENGTH = 12;        // 12 байт (96 бит) IV
    private static final int GCM_TAG_LENGTH = 16;       // 16 байт (128 бит) тег

    @PostConstruct
    private void init() {
        byte[] decodedKey = Base64.getDecoder().decode(aesSecret);
        this.secretKeySpec = new SecretKeySpec(decodedKey, "AES");
    }

    public String decrypt(String encryptedBase64) {
        try {
            byte[] decodedEncrypted = Base64.getDecoder().decode(encryptedBase64);

            if (decodedEncrypted.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("Payload слишком короткий для GCM.");
            }

            // IV = первые 12 байт
            byte[] iv = Arrays.copyOfRange(decodedEncrypted, 0, GCM_IV_LENGTH);
            // Остальное — cipherText + tag (GCM сам разберётся)
            byte[] cipherTextAndTag = Arrays.copyOfRange(decodedEncrypted, GCM_IV_LENGTH, decodedEncrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv); // длина в битах
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherTextAndTag);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифровки: " + e.getMessage(), e);
        }
    }
}
