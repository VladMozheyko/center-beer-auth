package fr.mossaab.security.config;

import java.net.URL;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.*;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.payment.VerifiedPurchaseRequest;

public class SignatureUtil {

    private static final String PUBLIC_KEY_URL = "https://www.rustore.ru/.well-known/public-key.pem";

    public static boolean verifySignature(VerifiedPurchaseRequest request) {
        try {
            String payload = new ObjectMapper().writeValueAsString(Map.of(
                    "orderId", request.getOrderId(),
                    "packageName", request.getPackageName(),
                    "productId", request.getProductId(),
                    "purchaseTime", request.getPurchaseTime(),
                    "purchaseToken", request.getPurchaseToken(),
                    "purchaseState", request.getPurchaseState(),
                    "purchaseType", request.getPurchaseType(),
                    "quantity", request.getQuantity(),
                    "developerPayload", request.getDeveloperPayload()
            ));

            System.out.println("🔐 [SIGNATURE CHECK] Payload: " + payload);
            System.out.println("🔐 [SIGNATURE CHECK] Signature: " + request.getSignature());

            byte[] signatureBytes = Base64.getDecoder().decode(request.getSignature());
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(getPublicKey());
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            boolean valid = sig.verify(signatureBytes);

            System.out.println("✅ Подпись корректна? → " + valid);
            return valid;

        } catch (Exception e) {
            System.err.println("❌ Ошибка при проверке подписи: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private static PublicKey getPublicKey() throws Exception {
        URL url = new URL(PUBLIC_KEY_URL);
        String pem = new BufferedReader(new InputStreamReader(url.openStream()))
                .lines().filter(line -> !line.startsWith("-----")).collect(Collectors.joining());

        byte[] keyBytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
