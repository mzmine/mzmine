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

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Compression related utilities
 */
public class CompressionUtils {

  /**
   * Decompress the zlib-compressed bytes and return an array of decompressed bytes
   * 
   */
  public static byte[] decompress(byte compressedBytes[]) throws DataFormatException {

    Inflater decompresser = new Inflater();

    decompresser.setInput(compressedBytes);

    byte[] resultBuffer = new byte[compressedBytes.length * 2];
    byte[] resultTotal = new byte[0];

    int resultLength = decompresser.inflate(resultBuffer);

    while (resultLength > 0) {
      byte previousResult[] = resultTotal;
      resultTotal = new byte[resultTotal.length + resultLength];
      System.arraycopy(previousResult, 0, resultTotal, 0, previousResult.length);
      System.arraycopy(resultBuffer, 0, resultTotal, previousResult.length, resultLength);
      resultLength = decompresser.inflate(resultBuffer);
    }

    decompresser.end();

    return resultTotal;
  }

}
