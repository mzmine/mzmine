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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import io.github.msdk.MSDKException;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.MSNumpress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.Deflater;

/**
 * <p>
 * Abstract MzMLPeaksEncoder class.
 * </p>
 *
 */
public abstract class MzMLPeaksEncoder {

  /**
   * <p>
   * encodeDouble.
   * </p>
   *
   * @param data an array of double.
   * @param compression a {@link MzMLCompressionType} object.
   * @return an array of byte.
   * @throws MSDKException if any.
   */
  public static byte[] encodeDouble(double[] data,MzMLCompressionType compression)
      throws MSDKException {

    byte[] encodedData = null;
    int encodedBytes;

    // MSNumpress compression if required
    switch (compression) {
      case NUMPRESS_LINPRED:
      case NUMPRESS_LINPRED_ZLIB:
        // Set encodedData's array to the maximum possible size, truncate it later
        encodedData = new byte[8 + (data.length * 5)];
        encodedBytes = MSNumpress.encodeLinear(data, data.length, encodedData,
            MSNumpress.optimalLinearFixedPoint(data, data.length));
        if (encodedBytes < 0)
          throw new MSDKException("MSNumpress linear encoding failed");
        encodedData = Arrays.copyOf(encodedData, encodedBytes);
        break;
      case NUMPRESS_POSINT:
      case NUMPRESS_POSINT_ZLIB:
        encodedData = new byte[data.length * 5];
        encodedBytes = MSNumpress.encodePic(data, data.length, encodedData);
        if (encodedBytes < 0)
          throw new MSDKException("MSNumpress positive integer encoding failed");
        encodedData = Arrays.copyOf(encodedData, encodedBytes);
        break;
      case NUMPRESS_SHLOGF:
      case NUMPRESS_SHLOGF_ZLIB:
        encodedData = new byte[8 + (data.length * 2)];
        encodedBytes = MSNumpress.encodeSlof(data, data.length, encodedData,
            MSNumpress.optimalSlofFixedPoint(data, data.length));
        if (encodedBytes < 0)
          throw new MSDKException("MSNumpress short floating logarithm encoding failed");
        encodedData = Arrays.copyOf(encodedData, encodedBytes);
        break;
      default:
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : data) {
          buffer.putDouble(d);
        }
        encodedData = buffer.array();
        break;
    }

    // Zlib Compression if necessary
    switch (compression) {
      case NUMPRESS_LINPRED_ZLIB:
      case NUMPRESS_POSINT_ZLIB:
      case NUMPRESS_SHLOGF_ZLIB:
      case ZLIB:
        byte[] tmp = ZlibCompress(encodedData);
        return Base64.getEncoder().encode(tmp);
      default:
        return Base64.getEncoder().encode(encodedData);
    }

  }

  /**
   * <p>
   * encodeFloat.
   * </p>
   *
   * @param data an array of float.
   * @param compression a {@link MzMLCompressionType} object.
   * @return an array of byte.
   * @throws MSDKException if any.
   */
  public static byte[] encodeFloat(float[] data, MzMLCompressionType compression)
      throws MSDKException {

    byte[] encodedData = null;

    // MSNumpress compression if required
    switch (compression) {
      case NUMPRESS_LINPRED:
      case NUMPRESS_LINPRED_ZLIB:
      case NUMPRESS_POSINT:
      case NUMPRESS_POSINT_ZLIB:
      case NUMPRESS_SHLOGF:
      case NUMPRESS_SHLOGF_ZLIB:
        throw new MSDKException("MSNumpress compression not supported for float values");
      default:
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : data) {
          buffer.putFloat(f);
        }
        encodedData = buffer.array();
        break;
    }

    // Zlib Compression if necessary
    switch (compression) {
      case ZLIB:
        byte[] tmp = ZlibCompress(encodedData);
        return Base64.getEncoder().encode(tmp);
      default:
        return Base64.getEncoder().encode(encodedData);
    }

  }

  /**
   * Compressed source data using the Deflate algorithm.
   * 
   * @param uncompressedData Data to be compressed
   * @return Compressed data
   */
  private static byte[] ZlibCompress(byte[] uncompressedData) {
    byte[] data = null; // Decompress the data

    // create a temporary byte array big enough to hold the compressed data
    // with the worst compression (the length of the initial (uncompressed) data)
    // EDIT: if it turns out this byte array was not big enough, then double its size and try again.
    byte[] temp = new byte[uncompressedData.length / 2];
    int compressedBytes = temp.length;
    while (compressedBytes == temp.length) {
      // compress
      temp = new byte[temp.length * 2];
      Deflater compresser = new Deflater();
      compresser.setInput(uncompressedData);
      compresser.finish();
      compressedBytes = compresser.deflate(temp);
    }

    // create a new array with the size of the compressed data (compressedBytes)
    data = new byte[compressedBytes];
    System.arraycopy(temp, 0, data, 0, compressedBytes);

    return data;
  }

}
