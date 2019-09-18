package net.sf.mzmine.util;


import de.unijena.bioinf.babelms.utils.Base64;
import net.sf.mzmine.util.exceptions.DecryptionException;
import net.sf.mzmine.util.exceptions.EncryptionException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Simple helper class to do AES encryption on Strings
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class StringCrypter {
    private static final Logger LOGGER = Logger.getLogger(StringCrypter.class.getName());
    private static final String HASH_METHOD = "SHA-512";
    private static final String CRYPT_METHOD = "AES";


    private final SecretKeySpec PRIVATE_KEY;

    public StringCrypter() {
        PRIVATE_KEY = makeKey();
    }

    public StringCrypter(byte[] key) {
        PRIVATE_KEY = new SecretKeySpec(key, CRYPT_METHOD);
    }

    public StringCrypter(String base64Key) throws IOException {
        this(base64Decode(base64Key));
    }

    @Override
    public String toString() {
        return base64Encode(PRIVATE_KEY.getEncoded());
    }

    public byte[] toBytes() {
        return PRIVATE_KEY.getEncoded();
    }

    private static SecretKeySpec makeKey() {
        try {
            //todo enter random key here
//            byte[] randomBytes = HARDCODED_KEY_SOURCE;
            byte[] randomBytes = new byte[40];
            SecureRandom.getInstanceStrong().nextBytes(randomBytes);
            byte[] hashed = Arrays.copyOf(MessageDigest.getInstance(HASH_METHOD).digest(randomBytes), 16);
            return new SecretKeySpec(hashed, CRYPT_METHOD);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        StringCrypter e = new StringCrypter("JtJagVMd9fkvXdnmMCcfWkZT".getBytes());

        String originalPassword = "secret";
        System.out.println("Original password: " + originalPassword);
        String encryptedPassword = e.encrypt(originalPassword);
        System.out.println("Encrypted password: " + encryptedPassword);
        String decryptedPassword = e.decrypt(encryptedPassword);
        System.out.println("Decrypted password: " + decryptedPassword);
    }

    public String encrypt(String toEncrypt) throws EncryptionException {
        //this checks are just to prevent trouble wiht empty encrypted parementers that contain
        // no null checks
        if (toEncrypt == null || toEncrypt.isEmpty()) {
            LOGGER.warning("Skipped empty encryption try.");
            return toEncrypt;
        }

        try {
            Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
            cipher.init(Cipher.ENCRYPT_MODE, PRIVATE_KEY);
            byte[] encrypted = cipher.doFinal(toEncrypt.getBytes());
            return base64Encode(encrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.encodeBytes(bytes);
    }

    public String decrypt(String encrypted) throws DecryptionException {
        if (encrypted == null || encrypted.isEmpty()) {
            LOGGER.warning("Skipped empty decryption try.");
            return encrypted;
        }

        try {
            byte[] crypted2 = base64Decode(encrypted);

            Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
            cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
            byte[] cipherData2 = cipher.doFinal(crypted2);
            return new String(cipherData2);
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new DecryptionException(e);
        }
    }

    public static byte[] base64Decode(String property) throws IOException {
        return Base64.decode(property);
    }
}

