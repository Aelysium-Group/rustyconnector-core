package group.aelysium.rustyconnector.common.crypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class AES {
    private final SecretKey key;

    public AES(SecretKey key) {
        this.key = key;
    }

    public byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = createIv();
        cipher.init(Cipher.ENCRYPT_MODE, this.key, iv);
        byte[] encryptedBytes = cipher.doFinal(data);

        // Combine IV and encrypted bytes
        byte[] combined = new byte[iv.getIV().length + encryptedBytes.length];
        System.arraycopy(iv.getIV(), 0, combined, 0, iv.getIV().length);
        System.arraycopy(encryptedBytes, 0, combined, iv.getIV().length, encryptedBytes.length);

        return combined;
    }
    public String encrypt(String data) throws Exception {
        return new String(encrypt(data.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public byte[] decrypt(byte[] encryptedData) throws Exception {
        // Extract IV and encrypted bytes
        byte[] ivBytes = new byte[16];
        byte[] encryptedBytes = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 0, ivBytes, 0, 16);
        System.arraycopy(encryptedData, 16, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, this.key, iv);

        return cipher.doFinal(encryptedBytes);
    }
    public String decrypt(String data) throws Exception {
        return new String(encrypt(data.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }


    public static byte[] createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();

            return secretKey.getEncoded();
        } catch (Exception ignore) {}
        return "".getBytes();
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

    /**
     * The cryptor returned here is effectively worthless because there's no way to retrieve the AES key used.
     */
    public static AES DEFAULT_CRYPTOR = AES.from(AES.createKey());
}