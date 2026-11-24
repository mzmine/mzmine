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

package io.github.mzmine.modules.io.import_rawdata_mzml.binary;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.jetbrains.annotations.NotNull;

/**
 * Decodes one mzML binary array after its descriptor has been read.
 */
public final class MzMLPeaksDecoder {

  private MzMLPeaksDecoder() {
  }

  /**
   * @param array binary text, numeric encoding and expected number of values
   * @return the decoded values in acquisition order
   */
  public static double @NotNull [] decodeToDouble(@NotNull final MzMLBinaryDataInfo array) {
    if (array.getEncodedLength() == 0) {
      return new double[0];
    }
    final byte[] decoded = decodeBase64(array.getXmlBinaryContent());
    final byte[] bytes = array.getCompressionType().isZlibCompressed() ? inflate(decoded) : decoded;
    final double[] values = new double[array.getArrayLength()];
    final int count = switch (array.getCompressionType()) {
      case NUMPRESS_LINPRED, NUMPRESS_LINPRED_ZLIB ->
          values.length == 0 && bytes.length == Double.BYTES ? 0
              : MSNumpress.decodeLinear(bytes, bytes.length, values);
      case NUMPRESS_POSINT, NUMPRESS_POSINT_ZLIB ->
          MSNumpress.decodePic(bytes, bytes.length, values);
      case NUMPRESS_SHLOGF, NUMPRESS_SHLOGF_ZLIB ->
          MSNumpress.decodeSlof(bytes, bytes.length, values);
      case NO_COMPRESSION, ZLIB -> {
        readNumbers(bytes, array.getBitLength(), values);
        yield values.length;
      }
    };
    if (count != values.length) {
      throw new IllegalArgumentException("Expected " + values.length + " values, decoded " + count);
    }
    return values;
  }

  private static byte @NotNull [] decodeBase64(@NotNull final String text) {
    try {
      return Base64.getDecoder().decode(text);
    } catch (IllegalArgumentException e) {
      // Some converters wrap the payload. Only XML whitespace is valid inside base64.
      final String compact = text.replaceAll("[ \\t\\r\\n]+", "");
      if (compact.length() == text.length()) {
        throw e;
      }
      return Base64.getDecoder().decode(compact);
    }
  }

  private static void readNumbers(final byte @NotNull [] bytes,
      @NotNull final MzMLBitLength precision, final double @NotNull [] values) {
    if ((long) values.length * (precision.bits() / Byte.SIZE) != bytes.length) {
      throw new IllegalArgumentException(
          "Binary byte count does not match the declared array length");
    }
    final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    switch (precision) {
      case SIXTEEN_BIT_FLOAT -> {
        for (int i = 0; i < values.length; i++) {
          values[i] = Float.float16ToFloat(buffer.getShort());
        }
      }
      case THIRTY_TWO_BIT_FLOAT -> {
        for (int i = 0; i < values.length; i++) {
          values[i] = buffer.getFloat();
        }
      }
      case SIXTY_FOUR_BIT_FLOAT -> {
        for (int i = 0; i < values.length; i++) {
          values[i] = buffer.getDouble();
        }
      }
      case THIRTY_TWO_BIT_INTEGER -> {
        for (int i = 0; i < values.length; i++) {
          values[i] = buffer.getInt();
        }
      }
      case SIXTY_FOUR_BIT_INTEGER -> {
        for (int i = 0; i < values.length; i++) {
          values[i] = buffer.getLong();
        }
      }
    }
  }

  private static byte @NotNull [] inflate(final byte @NotNull [] bytes) {
    try (final var inflater = new Inflater()) {
      inflater.setInput(bytes);
      final var output = new ByteArrayOutputStream((int) (bytes.length * 2.5));
      final byte[] buffer = new byte[1024];
      while (!inflater.finished()) {
        final int count = inflater.inflate(buffer);
        if (count == 0 && !inflater.finished()) {
          throw new IllegalArgumentException("Incomplete or unsupported zlib stream");
        }
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    } catch (DataFormatException e) {
      throw new IllegalArgumentException("Invalid zlib data", e);
    }
  }
}
