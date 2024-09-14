package group.aelysium.rustyconnector.common.crypt;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class ECC {
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    protected ECC(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return new String(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public String decrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    /**
     * Fetches the public and private keys from the resource path.
     * @param resourcePath The path of the resource. you should not include the fill extension.
     *                     This method will append ".public" and ".private" to the string you provide and use that as the path to fetch.
     */
    public static ECC fromResource(@NotNull String resourcePath) {
        PublicKey publicKey;
        PrivateKey privateKey;

        try {
            try (InputStream is = ECC.class.getResourceAsStream(resourcePath)) {
                byte[] keyBytes = is.readAllBytes();
                String keyString = new String(keyBytes, StandardCharsets.UTF_8)
                        .replaceAll("-----[A-Za-z\\s]*-----", "")
                        .replaceAll("\\s+", "");

                byte[] decoded = java.util.Base64.getDecoder().decode(keyString);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                publicKey = keyFactory.generatePublic(spec);
            }
            try (InputStream is = ECC.class.getResourceAsStream(resourcePath)) {
                byte[] keyBytes = is.readAllBytes();
                String keyString = new String(keyBytes, StandardCharsets.UTF_8)
                        .replaceAll("-----[A-Za-z\\s]*-----", "")
                        .replaceAll("\\s+", "");

                byte[] decoded = java.util.Base64.getDecoder().decode(keyString);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                privateKey = keyFactory.generatePrivate(spec);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ECC(publicKey, privateKey);
    }
}