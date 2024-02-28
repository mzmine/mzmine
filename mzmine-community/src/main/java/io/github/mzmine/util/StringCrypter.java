/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mzmine.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import io.github.mzmine.util.exceptions.DecryptionException;
import io.github.mzmine.util.exceptions.EncryptionException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Simple helper class to do AES encryption on Strings
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class StringCrypter {
  private static final Logger loggerGER = Logger.getLogger(StringCrypter.class.getName());
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
      byte[] randomBytes = new byte[40];
      SecureRandom.getInstanceStrong().nextBytes(randomBytes);
      byte[] hashed = Arrays.copyOf(MessageDigest.getInstance(HASH_METHOD).digest(randomBytes), 16);
      return new SecretKeySpec(hashed, CRYPT_METHOD);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public String encrypt(String toEncrypt) throws EncryptionException {
    // this checks are just to prevent trouble wiht empty encrypted
    // parementers that contain
    // no null checks
    if (toEncrypt == null || toEncrypt.isEmpty()) {
      loggerGER.warning("Skipped empty encryption try.");
      return toEncrypt;
    }

    try {
      Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
      cipher.init(Cipher.ENCRYPT_MODE, PRIVATE_KEY);
      byte[] encrypted = cipher.doFinal(toEncrypt.getBytes());
      return base64Encode(encrypted);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException e) {
      throw new EncryptionException(e);
    }
  }

  public static String base64Encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public String decrypt(String encrypted) throws DecryptionException {
    if (encrypted == null || encrypted.isEmpty()) {
      loggerGER.warning("Skipped empty decryption try.");
      return encrypted;
    }

    try {
      byte[] crypted2 = base64Decode(encrypted);

      Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
      cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
      byte[] cipherData2 = cipher.doFinal(crypted2);
      return new String(cipherData2);
    } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException e) {
      throw new DecryptionException(e);
    }
  }

  public static byte[] base64Decode(String property) throws IOException {
    return Base64.getDecoder().decode(property);
  }
}
