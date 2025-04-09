package fr.mossaab.security.config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
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
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            byte[] decodedEncrypted = Base64.getDecoder().decode(encryptedBase64);
            byte[] decryptedBytes = cipher.doFinal(decodedEncrypted);

            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифровки: " + e.getMessage(), e);
        }
    }
}
