package group.aelysium.rustyconnector.common.crypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AES {
    private final SecretKey key;

    public AES(SecretKey key) {
        this.key = key;
    }

    /**
     * Encrypts the data into a Base64 encoded string.
     * @param data The data to encrypt.
     * @return A Base64 encoded string.
     * @throws Exception If there was an issue.
     */
    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = createIv();
        cipher.init(Cipher.ENCRYPT_MODE, this.key, iv);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted bytes
        byte[] combined = new byte[iv.getIV().length + encryptedBytes.length];
        System.arraycopy(iv.getIV(), 0, combined, 0, iv.getIV().length);
        System.arraycopy(encryptedBytes, 0, combined, iv.getIV().length, encryptedBytes.length);

        return new String(Base64.getEncoder().encode(combined), StandardCharsets.UTF_8);
    }
    public String decrypt(String base64EncryptedData) throws Exception {
        byte[] decodedBytes = Base64.getDecoder().decode(base64EncryptedData);
        // Extract IV and encrypted bytes
        byte[] ivBytes = new byte[16];
        byte[] encryptedBytes = new byte[decodedBytes.length - 16];
        System.arraycopy(decodedBytes, 0, ivBytes, 0, 16);
        System.arraycopy(decodedBytes, 16, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, this.key, iv);

        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
    }

    public static byte[] createKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();

        return secretKey.getEncoded();
    }

    public static AES from(byte[] key) {
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        return new AES(secretKey);
    }

    private static IvParameterSpec createIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}