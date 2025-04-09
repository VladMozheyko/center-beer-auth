package fr.mossaab.security.config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
            System.out.println("🔐 Base64 decoded length: " + decodedEncrypted.length);

            if (decodedEncrypted.length % 16 != 0) {
                throw new IllegalArgumentException("Payload не кратен 16 байтам (AES). Возможно, поврежден.");
            }

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedBytes = cipher.doFinal(decodedEncrypted);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифровки: " + e.getMessage(), e);
        }
    }

}
