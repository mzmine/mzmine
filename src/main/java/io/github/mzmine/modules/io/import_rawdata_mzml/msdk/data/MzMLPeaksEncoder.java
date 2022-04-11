/*
 * (C) Copyright 2015-2016 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
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
