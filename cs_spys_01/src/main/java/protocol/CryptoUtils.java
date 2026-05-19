package protocol;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private static byte[] getKey() {
        String envKey = System.getenv("ENCRYPT_KEY");

        if (envKey == null || envKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "CRITICAL ERROR: ENCRYPT_KEY environment variable is not set. Cannot proceed with cryptographic operations."
            );
        }

        byte[] keyBytes = envKey.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "CRITICAL ERROR: ENCRYPT_KEY length must be exactly 16, 24, or 32 bytes long for AES."
            );
        }

        return keyBytes;
    }

    public static byte[] encrypt(byte[] data) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(getKey(), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);

        byte[] result = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
        return result;
    }

    public static byte[] decrypt(byte[] encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.length < IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data length.");
        }

        byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
        byte[] cipherText = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);

        SecretKeySpec keySpec = new SecretKeySpec(getKey(), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(cipherText);
    }
}