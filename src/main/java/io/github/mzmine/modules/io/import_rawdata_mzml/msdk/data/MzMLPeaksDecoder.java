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

import com.google.common.io.LittleEndianDataInputStream;
import io.github.msdk.MSDKException;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.ByteBufferInputStream;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.MSNumpress;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterInputStream;
import org.apache.commons.io.IOUtils;

/**
 * <p>
 * MzMLIntensityPeaksDecoder class.
 * </p>
 */
public class MzMLPeaksDecoder {

  /**
   * Converts a base64 encoded mz or intensity string used in mzML files to an array of floats. If
   * the original precision was 64 bit, you still get floats as output.
   *
   * @param binaryDataInfo meta-info about the compressed data
   * @throws DataFormatException if any.
   * @throws IOException if any.
   * @return a float array containing the decoded values
   * @throws MSDKException if any.
   * @param inputStream a {@link InputStream} object.
   * @param data an array of float.
   */
  public static float[] decodeToFloat(InputStream inputStream, MzMLBinaryDataInfo binaryDataInfo,
      float[] data) throws DataFormatException, IOException, MSDKException {

    int lengthIn = binaryDataInfo.getEncodedLength();
    int numPoints = binaryDataInfo.getArrayLength();
    InputStream is = null;

    if (inputStream instanceof ByteBufferInputStream) {
      ByteBufferInputStream mappedByteBufferInputStream = (ByteBufferInputStream) inputStream;
      mappedByteBufferInputStream.constrain(binaryDataInfo.getPosition(), lengthIn);
      is = Base64.getDecoder().wrap(mappedByteBufferInputStream);
    } else {
      is = Base64.getDecoder().wrap(inputStream);
    }

    // for some reason there sometimes might be zero length <peaks> tags
    // (ms2 usually)
    // in this case we just return an empty result
    if (lengthIn == 0) {
      return new float[0];
    }

    InflaterInputStream iis = null;
    LittleEndianDataInputStream dis = null;
    byte[] bytes = null;

    if (data == null || data.length < numPoints)
      data = new float[numPoints];

    // first check for zlib compression, inflation must be done before
    // NumPress
    if (binaryDataInfo.getCompressionType() != null) {
      switch (binaryDataInfo.getCompressionType()) {
        case ZLIB:
        case NUMPRESS_LINPRED_ZLIB:
        case NUMPRESS_POSINT_ZLIB:
        case NUMPRESS_SHLOGF_ZLIB:
          iis = new InflaterInputStream(is);
          dis = new LittleEndianDataInputStream(iis);
          break;
        default:
          dis = new LittleEndianDataInputStream(is);
          break;
      }

      // Now we can check for NumPress
      int numDecodedDoubles;
      switch (binaryDataInfo.getCompressionType()) {
        case NUMPRESS_LINPRED:
        case NUMPRESS_LINPRED_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeLinear(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress linear decoder failed");
          }
          return data;
        case NUMPRESS_POSINT:
        case NUMPRESS_POSINT_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodePic(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress positive integer decoder failed");
          }
          return data;
        case NUMPRESS_SHLOGF:
        case NUMPRESS_SHLOGF_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeSlof(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress short logged float decoder failed");
          }
          return data;
        default:
          break;
      }
    } else {
      dis = new LittleEndianDataInputStream(is);
    }

    Integer precision;
    switch (binaryDataInfo.getBitLength()) {
      case THIRTY_TWO_BIT_FLOAT:
      case THIRTY_TWO_BIT_INTEGER:
        precision = 32;
        break;
      case SIXTY_FOUR_BIT_FLOAT:
      case SIXTY_FOUR_BIT_INTEGER:
        precision = 64;
        break;
      default:
        dis.close();
        throw new IllegalArgumentException(
            "Precision MUST be specified and be either 32-bit or 64-bit, "
                + "if MS-NUMPRESS compression was not used");
    }

    try {
      switch (precision) {
        case (32): {
          
          for (int i = 0; i < numPoints; i++) {
            data[i] = dis.readFloat();
          }
          break;
        }
        case (64): {

          for (int i = 0; i < numPoints; i++) {
            data[i] = (float) dis.readDouble();
          }
          break;
        }
        default: {
          dis.close();
          throw new IllegalArgumentException(
              "Precision can only be 32/64 bits, other values are not valid.");
        }
      }
    } catch (EOFException eof) {
      // If the stream reaches EOF unexpectedly, it is probably because the particular
      // scan/chromatogram didn't pass the Predicate
      throw new MSDKException(
          "Couldn't obtain values. Please make sure the scan/chromatogram passes the Predicate.");
    } finally {
      dis.close();
    }

    return data;
  }

  /**
   * Converts a base64 encoded mz or intensity string used in mzML files to an array of doubles. If
   * the original precision was 32 bit, you still get doubles as output.
   *
   * @param binaryDataInfo meta-info about encoded data
   * @throws DataFormatException if any.
   * @throws IOException if any.
   * @return a double array containing the decoded values
   * @throws MSDKException if any.
   * @param inputStream a {@link InputStream} object.
   * @param data an array of double.
   */
  public static double[] decodeToDouble(InputStream inputStream, MzMLBinaryDataInfo binaryDataInfo,
      double[] data) throws DataFormatException, IOException, MSDKException {

    int lengthIn = binaryDataInfo.getEncodedLength();
    int numPoints = binaryDataInfo.getArrayLength();

    InputStream is = null;

    if (inputStream instanceof ByteBufferInputStream) {
      ByteBufferInputStream mappedByteBufferInputStream = (ByteBufferInputStream) inputStream;
      mappedByteBufferInputStream.constrain(binaryDataInfo.getPosition(), lengthIn);
      is = Base64.getDecoder().wrap(mappedByteBufferInputStream);
    } else {
      is = Base64.getDecoder().wrap(inputStream);
    }

    // for some reason there sometimes might be zero length <peaks> tags
    // (ms2 usually)
    // in this case we just return an empty result
    if (lengthIn == 0) {
      return new double[0];
    }

    InflaterInputStream iis = null;
    LittleEndianDataInputStream dis = null;
    byte[] bytes = null;

    if (data == null || data.length < numPoints)
      data = new double[numPoints];

    // first check for zlib compression, inflation must be done before
    // NumPress
    if (binaryDataInfo.getCompressionType() != null) {
      switch (binaryDataInfo.getCompressionType()) {
        case ZLIB:
        case NUMPRESS_LINPRED_ZLIB:
        case NUMPRESS_POSINT_ZLIB:
        case NUMPRESS_SHLOGF_ZLIB:
          iis = new InflaterInputStream(is);
          dis = new LittleEndianDataInputStream(iis);
          break;

        default:
          dis = new LittleEndianDataInputStream(is);
          break;
      }

      // Now we can check for NumPress
      int numDecodedDoubles;
      switch (binaryDataInfo.getCompressionType()) {
        case NUMPRESS_LINPRED:
        case NUMPRESS_LINPRED_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeLinear(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress linear decoder failed");
          }
          return data;
        case NUMPRESS_POSINT:
        case NUMPRESS_POSINT_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodePic(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress positive integer decoder failed");
          }
          return data;
        case NUMPRESS_SHLOGF:
        case NUMPRESS_SHLOGF_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeSlof(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress short logged float decoder failed");
          }
          return data;
        default:
          break;
      }
    } else {
      dis = new LittleEndianDataInputStream(is);
    }

    Integer precision;
    switch (binaryDataInfo.getBitLength()) {
      case THIRTY_TWO_BIT_FLOAT:
      case THIRTY_TWO_BIT_INTEGER:
        precision = 32;
        break;
      case SIXTY_FOUR_BIT_FLOAT:
      case SIXTY_FOUR_BIT_INTEGER:
        precision = 64;
        break;
      default:
        dis.close();
        throw new IllegalArgumentException(
            "Precision MUST be specified and be either 32-bit or 64-bit, "
                + "if MS-NUMPRESS compression was not used");
    }

    try {
      switch (precision) {
        case (32): {
          int asInt;

          for (int i = 0; i < numPoints; i++) {
            asInt = dis.readInt();
            data[i] = Float.intBitsToFloat(asInt);
          }
          break;
        }
        case (64): {
          long asLong;

          for (int i = 0; i < numPoints; i++) {
            asLong = dis.readLong();
            data[i] = Double.longBitsToDouble(asLong);
          }
          break;
        }
      }
    } catch (EOFException eof) {
      // If the stream reaches EOF unexpectedly, it is probably because the particular
      // scan/chromatogram didn't pass the Predicate
      throw new MSDKException(
          "Couldn't obtain values. Please make sure the scan/chromatogram passes the Predicate.");
    } finally {
      dis.close();
    }
    return data;
  }

}
