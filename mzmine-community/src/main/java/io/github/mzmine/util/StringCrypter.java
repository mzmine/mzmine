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

import io.github.mzmine.util.exceptions.DecryptionException;
import io.github.mzmine.util.exceptions.EncryptionException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple helper class to encrypt strings with AES.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class StringCrypter {
  private static final Logger loggerGER = Logger.getLogger(StringCrypter.class.getName());
  private static final String HASH_METHOD = "SHA-512";
  private static final String KEY_ALGORITHM = "AES";
  private static final String LEGACY_CRYPT_METHOD = "AES";
  private static final String GCM_CRYPT_METHOD = "AES/GCM/NoPadding";
  private static final String GCM_PREFIX = "GCM:";
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final SecretKeySpec PRIVATE_KEY;

  public StringCrypter() {
    PRIVATE_KEY = makeKey();
  }

  public StringCrypter(final byte @NotNull [] key) {
    PRIVATE_KEY = new SecretKeySpec(key, KEY_ALGORITHM);
  }

  public StringCrypter(@NotNull final String base64Key) throws IOException {
    this(base64Decode(base64Key));
  }

  @Override
  public @NotNull String toString() {
    return base64Encode(PRIVATE_KEY.getEncoded());
  }

  public byte @NotNull [] toBytes() {
    return PRIVATE_KEY.getEncoded();
  }

  private static @NotNull SecretKeySpec makeKey() {
    try {
      final byte[] randomBytes = new byte[40];
      SECURE_RANDOM.nextBytes(randomBytes);
      final byte[] hashed = Arrays.copyOf(MessageDigest.getInstance(HASH_METHOD).digest(randomBytes),
          16);
      return new SecretKeySpec(hashed, KEY_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public @Nullable String encrypt(@Nullable final String toEncrypt) throws EncryptionException {
    // assumption: callers may persist empty password fields without null checks.
    if (toEncrypt == null || toEncrypt.isEmpty()) {
      loggerGER.warning("Skipped empty encryption try.");
      return toEncrypt;
    }

    try {
      final byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      SECURE_RANDOM.nextBytes(iv);

      final Cipher cipher = Cipher.getInstance(GCM_CRYPT_METHOD);
      final GCMParameterSpec gcmParameters = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, PRIVATE_KEY, gcmParameters);

      final byte[] encrypted = cipher.doFinal(toEncrypt.getBytes(StandardCharsets.UTF_8));
      final byte[] payload = Arrays.copyOf(iv, iv.length + encrypted.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return GCM_PREFIX + base64Encode(payload);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
             | InvalidAlgorithmParameterException | IllegalBlockSizeException
             | BadPaddingException e) {
      throw new EncryptionException(e);
    }
  }

  public static @NotNull String base64Encode(final byte @NotNull [] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public @Nullable String decrypt(@Nullable final String encrypted) throws DecryptionException {
    if (encrypted == null || encrypted.isEmpty()) {
      loggerGER.warning("Skipped empty decryption try.");
      return encrypted;
    }

    try {
      if (encrypted.startsWith(GCM_PREFIX)) {
        final byte[] payload = base64Decode(encrypted.substring(GCM_PREFIX.length()));
        if (payload.length <= GCM_IV_LENGTH_BYTES) {
          throw new IOException("Invalid AES-GCM payload.");
        }

        final byte[] iv = Arrays.copyOf(payload, GCM_IV_LENGTH_BYTES);
        final byte[] ciphertext = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);
        final Cipher cipher = Cipher.getInstance(GCM_CRYPT_METHOD);
        final GCMParameterSpec gcmParameters = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY, gcmParameters);
        final byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
      }

      // decision: keep legacy AES/ECB decryption so existing conf.xml values still load.
      final byte[] ciphertext = base64Decode(encrypted);
      final Cipher cipher = Cipher.getInstance(LEGACY_CRYPT_METHOD);
      cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
      return new String(cipher.doFinal(ciphertext));
    } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
             | InvalidAlgorithmParameterException | IllegalBlockSizeException
             | BadPaddingException e) {
      throw new DecryptionException(e);
    }
  }

  public static byte @NotNull [] base64Decode(@NotNull final String property) throws IOException {
    return Base64.getDecoder().decode(property);
  }
}
