/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.util.exceptions.DecryptionException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class StringCrypterTest {

  @Test
  void encryptUsesVersionedAesGcmCiphertext() throws Exception {
    final StringCrypter crypter = new StringCrypter();

    final String encrypted = crypter.encrypt("hunter2");
    final String encryptedAgain = crypter.encrypt("hunter2");

    assertNotNull(encrypted);
    assertTrue(encrypted.startsWith("GCM:"));
    assertNotEquals(encrypted, encryptedAgain);
    assertEquals("hunter2", crypter.decrypt(encrypted));
  }

  @Test
  void decryptSupportsLegacyAesCiphertext() throws Exception {
    final StringCrypter crypter = new StringCrypter();

    final String legacyCiphertext = encryptLegacy(crypter.toBytes(), "legacy-secret");

    assertEquals("legacy-secret", crypter.decrypt(legacyCiphertext));
  }

  @Test
  void decryptRejectsInvalidGcmPayload() {
    final StringCrypter crypter = new StringCrypter();
    final String truncatedPayload = "GCM:" + StringCrypter.base64Encode(new byte[4]);

    assertThrows(DecryptionException.class, () -> crypter.decrypt(truncatedPayload));
  }

  private static String encryptLegacy(final byte[] key, final String plaintext) throws Exception {
    final Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
    return StringCrypter.base64Encode(cipher.doFinal(plaintext.getBytes()));
  }
}
