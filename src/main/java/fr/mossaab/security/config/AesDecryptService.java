package fr.mossaab.security.config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
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

    @PostConstruct
    private void init() {
        byte[] decodedKey = Base64.getDecoder().decode(aesSecret);
        this.secretKeySpec = new SecretKeySpec(decodedKey, "AES");
    }

    public String decrypt(String encryptedBase64) {
        try {
            byte[] decodedEncrypted = Base64.getDecoder().decode(encryptedBase64);

            if (decodedEncrypted.length < 16) {
                throw new IllegalArgumentException("Слишком короткий payload, отсутствует IV.");
            }

            // Первые 16 байт — IV
            byte[] iv = Arrays.copyOfRange(decodedEncrypted, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(decodedEncrypted, 16, decodedEncrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифровки: " + e.getMessage(), e);
        }
    }


}
