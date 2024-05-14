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
package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util;

/**
 * <p>
 * MSNumpressDouble class.
 * </p>
 */
public class MSNumpress {

  /// PSI-MS obo accession numbers.
  /** Constant <code>ACC_NUMPRESS_LINEAR="MS:1002312"</code> */
  public static final String ACC_NUMPRESS_LINEAR = "MS:1002312";
  /** Constant <code>ACC_NUMPRESS_PIC="MS:1002313"</code> */
  public static final String ACC_NUMPRESS_PIC = "MS:1002313";
  /** Constant <code>ACC_NUMPRESS_SLOF="MS:1002314"</code> */
  public static final String ACC_NUMPRESS_SLOF = "MS:1002314";


  /**
   * Convenience function for decoding binary data encoded by MSNumpress. If the passed cvAccession
   * is one of
   * <p>
   * ACC_NUMPRESS_LINEAR = "MS:1002312" ACC_NUMPRESS_PIC = "MS:1002313" ACC_NUMPRESS_SLOF =
   * "MS:1002314"
   * <p>
   * the corresponding decode function will be called.
   *
   * @param cvAccession The PSI-MS obo CV accession of the encoded data.
   * @param data array of double to be encoded
   * @param dataSize number of doubles from data to encode
   * @return The decoded doubles
   */
  public static double[] decode(String cvAccession, byte[] data, int dataSize) {

    switch (cvAccession) {
      case ACC_NUMPRESS_LINEAR: {
        double[] buffer = new double[dataSize * 2];
        int nbrOfDoubles = MSNumpress.decodeLinear(data, dataSize, buffer);
        double[] result = new double[nbrOfDoubles];
        System.arraycopy(buffer, 0, result, 0, nbrOfDoubles);
        return result;

      }
      case ACC_NUMPRESS_SLOF: {
        double[] result = new double[dataSize / 2];
        MSNumpress.decodeSlof(data, dataSize, result);
        return result;

      }
      case ACC_NUMPRESS_PIC: {
        double[] buffer = new double[dataSize * 2];
        int nbrOfDoubles = MSNumpress.decodePic(data, dataSize, buffer);
        double[] result = new double[nbrOfDoubles];
        System.arraycopy(buffer, 0, result, 0, nbrOfDoubles);
        return result;

      }
    }

    throw new IllegalArgumentException("'" + cvAccession + "' is not a numpress compression term");
  }


  /**
   * This encoding works on a 4 byte integer, by truncating initial zeros or ones. If the initial
   * (most significant) half byte is 0x0 or 0xf, the number of such halfbytes starting from the most
   * significant is stored in a halfbyte. This initial count is then followed by the rest of the
   * ints halfbytes, in little-endian order. A count halfbyte c of
   * <p>
   * 0 &lt;= c &lt;= 8 is interpreted as an initial c 0x0 halfbytes 9 &lt;= c &lt;= 15 is
   * interpreted as an initial (c-8) 0xf halfbytes
   * <p>
   * Ex: int c rest 0 =&gt; 0x8 -1 =&gt; 0xf 0xf 23 =&gt; 0x6 0x7 0x1
   *
   * @param x the int to be encoded
   * @param res the byte array were halfbytes are stored
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @param resOffset position in res were halfbytes are written
   * @return the number of resulting halfbytes
   */
  protected static int encodeInt(long x, byte[] res, int resOffset) {
    byte i, l;
    long m;
    long mask = 0xf0000000;
    long init = x & mask;

    if (init == 0) {
      l = 8;
      for (i = 0; i < 8; i++) {
        m = mask >> (4 * i);
        if ((x & m) != 0) {
          l = i;
          break;
        }
      }
      res[resOffset] = l;
      for (i = l; i < 8; i++)
        res[resOffset + 1 + i - l] = (byte) (0xf & (x >> (4 * (i - l))));

      return 1 + 8 - l;

    } else if (init == mask) {
      l = 7;
      for (i = 0; i < 8; i++) {
        m = mask >> (4 * i);
        if ((x & m) != m) {
          l = i;
          break;
        }
      }
      res[resOffset] = (byte) (l | 8);
      for (i = l; i < 8; i++)
        res[resOffset + 1 + i - l] = (byte) (0xf & (x >> (4 * (i - l))));

      return 1 + 8 - l;

    } else {
      res[resOffset] = 0;
      for (i = 0; i < 8; i++)
        res[resOffset + 1 + i] = (byte) (0xf & (x >> (4 * i)));

      return 9;

    }
  }


  /**
   * <p>
   * encodeFixedPoint.
   * </p>
   *
   * @param fixedPoint a double.
   * @param result an array of byte.
   */
  public static void encodeFixedPoint(double fixedPoint, byte[] result) {
    long fp = Double.doubleToLongBits(fixedPoint);
    for (int i = 0; i < 8; i++) {
      result[7 - i] = (byte) ((fp >> (8 * i)) & 0xff);
    }
  }


  /**
   * <p>
   * decodeFixedPoint.
   * </p>
   *
   * @param data an array of byte.
   * @return a double.
   */
  public static double decodeFixedPoint(byte[] data) {
    long fp = 0;
    for (int i = 0; i < 8; i++) {
      fp = fp | ((0xFFL & data[7 - i]) << (8 * i));
    }
    return Double.longBitsToDouble(fp);
  }


  /////////////////////////////////////////////////////////////////////////////////


  /**
   * <p>
   * optimalLinearFixedPoint.
   * </p>
   *
   * @param data an array of double.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @return a double.
   */
  public static double optimalLinearFixedPoint(double[] data, int dataSize) {
    if (dataSize == 0)
      return 0;
    if (dataSize == 1)
      return Math.floor(0xFFFFFFFFL / data[0]);
    double maxDouble = Math.max(data[0], data[1]);

    for (int i = 2; i < dataSize; i++) {
      double extrapol = data[i - 1] + (data[i - 1] - data[i - 2]);
      double diff = data[i] - extrapol;
      maxDouble = Math.max(maxDouble, Math.ceil(Math.abs(diff) + 1));
    }

    return Math.floor(0x7FFFFFFFL / maxDouble);
  }


  /**
   * Encodes the doubles in data by first using a - lossy conversion to a 4 byte 5 decimal fixed
   * point repressentation - storing the residuals from a linear prediction after first to values -
   * encoding by encodeInt (see above)
   * <p>
   * The resulting binary is maximally 8 + dataSize * 5 bytes, but much less if the data is
   * reasonably smooth on the first order.
   * <p>
   * This encoding is suitable for typical m/z or retention time binary arrays. On a test set, the
   * encoding was empirically show to be accurate to at least 0.002 ppm.
   *
   * @param data array of doubles to be encoded
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param result array were resulting bytes should be stored
   * @param fixedPoint the scaling factor used for getting the fixed point repr. This is stored in
   *        the binary and automatically extracted on decoding.
   * @return the number of encoded bytes
   */
  public static int encodeLinear(double[] data, int dataSize, byte[] result, double fixedPoint) {
    long[] ints = new long[3];
    int i, ri, halfByteCount, hbi;
    byte halfBytes[] = new byte[10];
    long extrapol, diff;

    encodeFixedPoint(fixedPoint, result);

    if (dataSize == 0)
      return 8;

    ints[1] = (long) (data[0] * fixedPoint + 0.5);
    for (i = 0; i < 4; i++) {
      result[8 + i] = (byte) ((ints[1] >> (i * 8)) & 0xff);
    }

    if (dataSize == 1)
      return 12;

    ints[2] = (long) (data[1] * fixedPoint + 0.5);
    for (i = 0; i < 4; i++) {
      result[12 + i] = (byte) ((ints[2] >> (i * 8)) & 0xff);
    }

    halfByteCount = 0;
    ri = 16;

    for (i = 2; i < dataSize; i++) {
      ints[0] = ints[1];
      ints[1] = ints[2];
      ints[2] = (long) (data[i] * fixedPoint + 0.5);
      extrapol = ints[1] + (ints[1] - ints[0]);
      diff = ints[2] - extrapol;
      halfByteCount += encodeInt(diff, halfBytes, halfByteCount);

      for (hbi = 1; hbi < halfByteCount; hbi += 2)
        result[ri++] = (byte) ((halfBytes[hbi - 1] << 4) | (halfBytes[hbi] & 0xf));

      if (halfByteCount % 2 != 0) {
        halfBytes[0] = halfBytes[halfByteCount - 1];
        halfByteCount = 1;
      } else
        halfByteCount = 0;

    }
    if (halfByteCount == 1)
      result[ri++] = (byte) (halfBytes[0] << 4);

    return ri;
  }


  /**
   * Decodes data encoded by encodeLinear.
   * <p>
   * result vector guaranteed to be shorter or equal to (|data| - 8) * 2
   * <p>
   * Note that this method may throw a ArrayIndexOutOfBoundsException if it deems the input data to
   * be corrupt, i.e. that the last encoded int does not use the last byte in the data. In addition
   * the last encoded int need to use either the last halfbyte, or the second last followed by a 0x0
   * halfbyte.
   *
   * @param data array of bytes to be decoded
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting doubles should be stored
   * @return the number of decoded doubles, or -1 if dataSize &lt; 4 or 4 &lt; dataSize &lt; 8
   */
  public static int decodeLinear(byte[] data, int dataSize, double[] result) {
    int ri = 2;
    long[] ints = new long[3];
    long extrapol;
    long y;
    IntDecoder dec = new IntDecoder(data, 16);

    if (dataSize < 8)
      return -1;
    double fixedPoint = decodeFixedPoint(data);
    if (dataSize < 12)
      return -1;

    ints[1] = 0;
    for (int i = 0; i < 4; i++) {
      ints[1] = ints[1] | ((0xFFL & data[8 + i]) << (i * 8));
    }
    result[0] = ints[1] / fixedPoint;

    if (dataSize == 12)
      return 1;
    if (dataSize < 16)
      return -1;

    ints[2] = 0;
    for (int i = 0; i < 4; i++) {
      ints[2] = ints[2] | ((0xFFL & data[12 + i]) << (i * 8));
    }
    result[1] = ints[2] / fixedPoint;

    while (dec.pos < dataSize) {
      if (dec.pos == (dataSize - 1) && dec.half)
        if ((data[dec.pos] & 0xf) != 0x8)
          break;

      ints[0] = ints[1];
      ints[1] = ints[2];
      ints[2] = dec.next();

      extrapol = ints[1] + (ints[1] - ints[0]);
      y = extrapol + ints[2];
      result[ri++] = y / fixedPoint;
      ints[2] = y;
    }

    return ri;
  }

  /**
   * Decodes data encoded by encodeLinear.
   * <p>
   * result vector guaranteed to be shorter or equal to (|data| - 8) * 2
   * <p>
   * Note that this method may throw a ArrayIndexOutOfBoundsException if it deems the input data to
   * be corrupt, i.e. that the last encoded int does not use the last byte in the data. In addition
   * the last encoded int need to use either the last halfbyte, or the second last followed by a 0x0
   * halfbyte.
   *
   * @param data array of bytes to be decoded
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting floats should be stored
   * @return the number of decoded floats, or -1 if dataSize &lt; 4 or 4 &lt; dataSize &lt; 8
   */
  public static int decodeLinear(byte[] data, int dataSize, float[] result) {
    int ri = 2;
    long[] ints = new long[3];
    long extrapol;
    long y;
    IntDecoder dec = new IntDecoder(data, 16);

    if (dataSize < 8)
      return -1;
    float fixedPoint = (float) decodeFixedPoint(data);
    if (dataSize < 12)
      return -1;

    ints[1] = 0;
    for (int i = 0; i < 4; i++) {
      ints[1] = ints[1] | ((0xFFL & data[8 + i]) << (i * 8));
    }
    result[0] = ints[1] / fixedPoint;

    if (dataSize == 12)
      return 1;
    if (dataSize < 16)
      return -1;

    ints[2] = 0;
    for (int i = 0; i < 4; i++) {
      ints[2] = ints[2] | ((0xFFL & data[12 + i]) << (i * 8));
    }
    result[1] = ints[2] / fixedPoint;

    while (dec.pos < dataSize) {
      if (dec.pos == (dataSize - 1) && dec.half)
        if ((data[dec.pos] & 0xf) != 0x8)
          break;

      ints[0] = ints[1];
      ints[1] = ints[2];
      ints[2] = dec.next();

      extrapol = ints[1] + (ints[1] - ints[0]);
      y = extrapol + ints[2];
      result[ri++] = y / fixedPoint;
      ints[2] = y;
    }

    return ri;
  }


  /////////////////////////////////////////////////////////////////////////////////

  /**
   * Encodes ion counts by simply rounding to the nearest 4 byte integer, and compressing each
   * integer with encodeInt.
   * <p>
   * The handleable range is therefore 0 -&gt; 4294967294. The resulting binary is maximally
   * dataSize * 5 bytes, but much less if the data is close to 0 on average.
   *
   * @param data array of doubles to be encoded
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param result array were resulting bytes should be stored
   * @return the number of encoded bytes
   */
  public static int encodePic(double[] data, int dataSize, byte[] result) {
    long count;
    int ri = 0;
    int hbi;
    byte halfBytes[] = new byte[10];
    int halfByteCount = 0;

    // printf("Encoding %d doubles\n", (int)dataSize);

    for (int i = 0; i < dataSize; i++) {
      count = (long) (data[i] + 0.5);
      halfByteCount += encodeInt(count, halfBytes, halfByteCount);

      for (hbi = 1; hbi < halfByteCount; hbi += 2)
        result[ri++] = (byte) ((halfBytes[hbi - 1] << 4) | (halfBytes[hbi] & 0xf));

      if (halfByteCount % 2 != 0) {
        halfBytes[0] = halfBytes[halfByteCount - 1];
        halfByteCount = 1;
      } else
        halfByteCount = 0;

    }
    if (halfByteCount == 1)
      result[ri++] = (byte) (halfBytes[0] << 4);

    return ri;
  }


  /**
   * Decodes data encoded by encodePic
   * <p>
   * result vector guaranteed to be shorter of equal to |data| * 2
   * <p>
   * Note that this method may throw a ArrayIndexOutOfBoundsException if it deems the input data to
   * be corrupt, i.e. that the last encoded int does not use the last byte in the data. In addition
   * the last encoded int need to use either the last halfbyte, or the second last followed by a 0x0
   * halfbyte.
   *
   * @param data array of bytes to be decoded (need memorycont. repr.)
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting doubles should be stored
   * @return the number of decoded doubles
   */
  public static int decodePic(byte[] data, int dataSize, double[] result) {
    int ri = 0;
    long count;
    IntDecoder dec = new IntDecoder(data, 0);

    while (dec.pos < dataSize) {
      if (dec.pos == (dataSize - 1) && dec.half)
        if ((data[dec.pos] & 0xf) != 0x8)
          break;

      count = dec.next();
      result[ri++] = count;
    }
    return ri;
  }

  /**
   * Decodes data encoded by encodePic
   * <p>
   * result vector guaranteed to be shorter of equal to |data| * 2
   * <p>
   * Note that this method may throw a ArrayIndexOutOfBoundsException if it deems the input data to
   * be corrupt, i.e. that the last encoded int does not use the last byte in the data. In addition
   * the last encoded int need to use either the last halfbyte, or the second last followed by a 0x0
   * halfbyte.
   *
   * @param data array of bytes to be decoded (need memorycont. repr.)
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting floats should be stored
   * @return the number of decoded floats
   */
  public static int decodePic(byte[] data, int dataSize, float[] result) {
    int ri = 0;
    long count;
    IntDecoder dec = new IntDecoder(data, 0);

    while (dec.pos < dataSize) {
      if (dec.pos == (dataSize - 1) && dec.half)
        if ((data[dec.pos] & 0xf) != 0x8)
          break;

      count = dec.next();
      result[ri++] = count;
    }
    return ri;
  }

  /**
   * Decodes data encoded by encodeSlof
   * <p>
   * The result vector will be exactly (|data| - 8) / 2 floats. returns the number of floats read,
   * or -1 is there is a problem decoding.
   *
   * @param data array of bytes to be decoded (need memorycont. repr.)
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting floats should be stored
   * @return the number of decoded floats
   */
  public static int decodeSlof(byte[] data, int dataSize, float[] result) {
    int x;
    int ri = 0;

    if (dataSize < 8)
      return -1;
    float fixedPoint = (float) decodeFixedPoint(data);

    if (dataSize % 2 != 0)
      return -1;

    for (int i = 8; i < dataSize; i += 2) {
      x = (0xff & data[i]) | ((0xff & data[i + 1]) << 8);
      result[ri++] = (float) (Math.exp(((float) (0xffff & x)) / fixedPoint) - 1);
    }
    return ri;
  }


  /////////////////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * optimalSlofFixedPoint.
   * </p>
   *
   * @param data an array of double.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @param dataSize a int.
   * @return a double.
   */
  public static double optimalSlofFixedPoint(double[] data, int dataSize) {
    if (dataSize == 0)
      return 0;

    double maxDouble = 1;
    double x;
    double fp;

    for (int i = 0; i < dataSize; i++) {
      x = Math.log(data[i] + 1);
      maxDouble = Math.max(maxDouble, x);
    }

    fp = Math.floor(0xFFFF / maxDouble);

    return fp;
  }

  /**
   * Encodes ion counts by taking the natural logarithm, and storing a fixed point representation of
   * this. This is calculated as
   * <p>
   * unsigned short fp = log(d+1) * fixedPoint + 0.5
   * <p>
   * the result vector is exactly |data| * 2 + 8 bytes long
   *
   * @param data array of doubles to be encoded
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param dataSize number of doubles from data to encode
   * @param result array were resulting bytes should be stored
   * @param fixedPoint the scaling factor used for getting the fixed point repr. This is stored in
   *        the binary and automatically extracted on decoding.
   * @return the number of encoded bytes
   */
  public static int encodeSlof(double[] data, int dataSize, byte[] result, double fixedPoint) {
    int x;
    int ri = 8;

    encodeFixedPoint(fixedPoint, result);

    for (int i = 0; i < dataSize; i++) {
      x = (int) (Math.log(data[i] + 1) * fixedPoint + 0.5);

      result[ri++] = (byte) (0xff & x);
      result[ri++] = (byte) (x >> 8);
    }
    return ri;
  }


  /**
   * Decodes data encoded by encodeSlof
   * <p>
   * The result vector will be exactly (|data| - 8) / 2 doubles. returns the number of doubles read,
   * or -1 is there is a problem decoding.
   *
   * @param data array of bytes to be decoded (need memorycont. repr.)
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting doubles should be stored
   * @return the number of decoded doubles
   */
  public static int decodeSlof(byte[] data, int dataSize, double[] result) {
    int x;
    int ri = 0;

    if (dataSize < 8)
      return -1;
    double fixedPoint = decodeFixedPoint(data);

    if (dataSize % 2 != 0)
      return -1;

    for (int i = 8; i < dataSize; i += 2) {
      x = (0xff & data[i]) | ((0xff & data[i + 1]) << 8);
      result[ri++] = Math.exp(((double) (0xffff & x)) / fixedPoint) - 1;
    }
    return ri;
  }

}


/**
 * Decodes ints from the half bytes in bytes. Lossless reverse of encodeInt, although not
 * symmetrical in input arguments.
 */
class IntDecoder {

  int pos = 0;
  boolean half = false;
  byte[] bytes;

  /**
   * <p>
   * Constructor for IntDecoder.
   * </p>
   *
   * @param _bytes an array of byte.
   * @param _pos a int.
   */
  public IntDecoder(byte[] _bytes, int _pos) {
    bytes = _bytes;
    pos = _pos;
  }

  /**
   * <p>
   * next.
   * </p>
   *
   * @return a long.
   */
  public long next() {
    int head;
    int i, n;
    long res = 0;
    long mask, m;
    int hb;

    if (!half)
      head = (0xff & bytes[pos]) >> 4;
    else
      head = 0xf & bytes[pos++];

    half = !half;

    if (head <= 8)
      n = head;
    else {
      // leading ones, fill in res
      n = head - 8;
      mask = 0xf0000000;
      for (i = 0; i < n; i++) {
        m = mask >> (4 * i);
        res = res | m;
      }
    }

    if (n == 8)
      return 0;

    for (i = n; i < 8; i++) {
      if (!half)
        hb = (0xff & bytes[pos]) >> 4;
      else
        hb = 0xf & bytes[pos++];

      res = res | (hb << ((i - n) * 4));
      half = !half;
    }

    return res;
  }
}

