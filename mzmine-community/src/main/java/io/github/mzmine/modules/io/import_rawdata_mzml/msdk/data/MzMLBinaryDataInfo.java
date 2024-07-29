/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>MzMLBinaryDataInfo class.</p>
 */
public class MzMLBinaryDataInfo {

  private final int encodedLength;
  private final int arrayLength;
  private long position;
  private @NotNull MzMLCompressionType compressionType;
  private MzMLBitLength bitLength;
  private MzMLArrayType arrayType;
  // the binary text content
  private String xmlBinaryContent;
  private String unitAccession;

  /**
   * <p>
   * Constructor for MzMLBinaryDataInfo
   * </p>
   *
   * @param encodedLength the length of the base64 encoded data (number of bytes)
   * @param arrayLength   number of data points in the decoded data array
   */
  public MzMLBinaryDataInfo(int encodedLength, int arrayLength) {
    this.encodedLength = encodedLength;
    this.arrayLength = arrayLength;
    this.compressionType = MzMLCompressionType.NO_COMPRESSION;
  }

  /**
   * <p>Getter for the field <code>bitLength</code>.</p>
   *
   * @return a {@link MzMLBitLength MzMLBitLength} corresponding to the bit length of the binary
   * data array
   */
  public MzMLBitLength getBitLength() {
    return bitLength;
  }

  /**
   * <p>
   * Sets the precision bit length of the binary data array
   * </p>
   *
   * @param bitLength a {@link MzMLBitLength} object.
   */
  public void setBitLength(MzMLBitLength bitLength) {
    this.bitLength = bitLength;
  }

  /**
   * <p>
   * Check if the given CV Parameter accession is that of bit length
   * </p>
   *
   * @param bitLengthAccession The CV Parameter accession as {@link String String}
   * @return true if the given accession is of a bit length CV Parameter, false otherwise
   */
  public boolean isBitLengthAccession(String bitLengthAccession) {
    return getBitLength(bitLengthAccession) != null;
  }

  /**
   * <p>
   * Gets the bit length for the given accession
   * </p>
   *
   * @param accession The CV Parameter accession as {@link String String}
   * @return a {@link MzMLBitLength MzMLBitLength} enum constant if the accession corresponds to a
   * valid bit length, null otherwise
   */
  public MzMLBitLength getBitLength(String accession) {
    for (MzMLBitLength bitLength : MzMLBitLength.values()) {
      if (bitLength.getValue().equals(accession)) {
        return bitLength;
      }
    }
    return null;
  }

  /**
   * <p>Getter for the field <code>compressionType</code>.</p>
   *
   * @return a {@link MzMLCompressionType MzMLCompressionType} corresponding to the compression of
   * the binary data array
   */
  public @NotNull MzMLCompressionType getCompressionType() {
    return compressionType;
  }

  /**
   * <p>
   * Sets the compression type of the binary data array
   * </p>
   *
   * @param compressionType a {@link MzMLCompressionType} object.
   */
  public void setCompressionType(@NotNull MzMLCompressionType compressionType) {
    this.compressionType = compressionType;
  }

  /**
   * <p>Getter for the field <code>arrayType</code>.</p>
   *
   * @return a {@link MzMLArrayType MzMLArrayType} enum constant corresponding to the the array type
   * of the binary data array
   */
  public MzMLArrayType getArrayType() {
    return arrayType;
  }

  /**
   * <p>
   * Sets the bit length of the binary data array
   * </p>
   *
   * @param arrayType a {@link MzMLArrayType} object.
   */
  public void setArrayType(MzMLArrayType arrayType) {
    this.arrayType = arrayType;
  }

  /**
   * <p>Getter for the field <code>position</code>.</p>
   *
   * @return The position of the binary array in the {@link java.io.InputStream InputStream}
   * corresponding to the MzML format data
   */
  public long getPosition() {
    return position;
  }

  /**
   * <p>
   * Set the position of the binary array in the {@link java.io.InputStream InputStream}
   * corresponding to the MzML format data Deprecated at the moment due to the problems with xml
   * reader random access in case of 3- and 4-byte characters
   * </p>
   *
   * @param position a <code>long</code> number defining the position of the binary array in the
   *                 {@link java.io.InputStream InputStream} corresponding to the MzML format data
   */
  @Deprecated
  public void setPosition(long position) {
    this.position = position;
  }

  /**
   * <p>Getter for the field <code>encodedLength</code>.</p>
   *
   * @return the length of the base64 encoded data (number of bytes)
   */
  public int getEncodedLength() {
    return encodedLength;
  }

  /**
   * <p>Getter for the field <code>arrayLength</code>.</p>
   *
   * @return the of data points in the decoded data array
   */
  public int getArrayLength() {
    return arrayLength;
  }

  /**
   * binary text content that is later parsed
   *
   * @param xmlBinaryContent the text content
   */
  public void setTextContent(final String xmlBinaryContent) {
    this.xmlBinaryContent = xmlBinaryContent;
  }

  /**
   * binary text content that is later parsed
   *
   * @return the text content
   */
  public String getXmlBinaryContent() {
    return xmlBinaryContent;
  }

  public @Nullable String getUnitAccession() {
    return unitAccession;
  }

  public void setUnitAccession(String unitAccession) {
    this.unitAccession = unitAccession;
  }
}
