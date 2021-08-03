/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import org.jetbrains.annotations.NotNull;

/**
 * <p>MzMLBinaryDataInfo class.</p>
 *
 */
public class MzMLBinaryDataInfo {

  private final int encodedLength;
  private final int arrayLength;
  private long position;
  private @NotNull
  MzMLCompressionType compressionType;
  private MzMLBitLength bitLength;
  private MzMLArrayType arrayType;

  /**
   * <p>
   * Constructor for MzMLBinaryDataInfo
   * </p>
   *
   * @param encodedLength the length of the base64 encoded data (number of bytes)
   * @param arrayLength number of data points in the decoded data array
   */
  public MzMLBinaryDataInfo(int encodedLength, int arrayLength) {
    this.encodedLength = encodedLength;
    this.arrayLength = arrayLength;
    this.compressionType = MzMLCompressionType.NO_COMPRESSION;
  }

  /**
   * <p>Getter for the field <code>bitLength</code>.</p>
   *
   * @return a {@link MzMLBitLength MzMLBitLength} corresponding to the
   *         bit length of the binary data array
   */
  public MzMLBitLength getBitLength() {
    return bitLength;
  }

  /**
   * <p>
   * Sets the precision bit length of the binary data array
   * </p>
   *
   * @param bitLengthAccession The CV Parameter accession as {@link String String}
   */
  public void setBitLength(String bitLengthAccession) {
    this.bitLength = getBitLength(bitLengthAccession);
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
   * @return a {@link MzMLBitLength MzMLBitLength} enum constant if the
   *         accession corresponds to a valid bit length, null otherwise
   */
  public MzMLBitLength getBitLength(String accession) {
    for (MzMLBitLength bitLength : MzMLBitLength.values()) {
      if (bitLength.getValue().equals(accession))
        return bitLength;
    }
    return null;
  }

  /**
   * <p>Getter for the field <code>compressionType</code>.</p>
   *
   * @return a {@link MzMLCompressionType MzMLCompressionType}
   *         corresponding to the compression of the binary data array
   */
  public MzMLCompressionType getCompressionType() {
    return compressionType;
  }

  /**
   * <p>
   * Sets the compression type of the binary data array
   * </p>
   *
   * @param compressionTypeAccession the CV Parameter accession as {@link String String}
   */
  public void setCompressionType(String compressionTypeAccession) {
    this.compressionType = getCompressionType(compressionTypeAccession);
  }

  /**
   * <p>
   * Sets the compression type of the binary data array
   * </p>
   *
   * @param compressionType a {@link MzMLCompressionType} object.
   */
  public void setCompressionType(MzMLCompressionType compressionType) {
    this.compressionType = compressionType;
  }

  /**
   * <p>
   * Check if the given CV Parameter accession is that of a compression method
   * </p>
   *
   * @param compressionTypeAccession The CV Parameter accession as {@link String String}
   * @return true if the given accession is of a compression type CV Parameter, false otherwise
   */
  public boolean isCompressionTypeAccession(String compressionTypeAccession) {
    return getCompressionType(compressionTypeAccession) != null;
  }

  /**
   * <p>
   * Gets the compression type for the given accession
   * </p>
   *
   * @param accession The CV Parameter accession as {@link String String}
   * @return a {@link MzMLCompressionType MzMLCompressionType} enum
   *         constant if the accession corresponds to a valid compression type, null otherwise
   */
  public MzMLCompressionType getCompressionType(String accession) {
    for (MzMLCompressionType compressionType : MzMLCompressionType.values()) {
      if (compressionType.getAccession().equals(accession))
        return compressionType;
    }
    return null;
  }

  /**
   * <p>Getter for the field <code>arrayType</code>.</p>
   *
   * @return a {@link MzMLArrayType MzMLArrayType} enum constant
   *         corresponding to the the array type of the binary data array
   */
  public MzMLArrayType getArrayType() {
    return arrayType;
  }

  /**
   * <p>
   * Sets the array type of the binary data array
   * </p>
   *
   * @param arrayTypeAccession the CV Parameter accession as {@link String String}
   */
  public void setArrayType(String arrayTypeAccession) {
    this.arrayType = getArrayType(arrayTypeAccession);
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
   * <p>
   * Check if the given CV Parameter accession is that of a binary data array type
   * </p>
   *
   * @param arrayTypeAccession the CV Parameter accession as {@link String String}
   * @return true if the given accession is of a binary data array type CV Parameter, false
   *         otherwise
   */
  public boolean isArrayTypeAccession(String arrayTypeAccession) {
    return getArrayType(arrayTypeAccession) != null;
  }

  /**
   * <p>
   * Gets the binary data array type for the given accession
   * </p>
   *
   * @param accession The CV Parameter accession as {@link String String}
   * @return a {@link MzMLArrayType MzMLArrayType} enum constant if the
   *         accession corresponds to a valid binary data array type, null otherwise
   */
  public MzMLArrayType getArrayType(String accession) {
    for (MzMLArrayType arrayType : MzMLArrayType.values()) {
      if (arrayType.getAccession().equals(accession))
        return arrayType;
    }
    return null;
  }

  /**
   * <p>Getter for the field <code>position</code>.</p>
   *
   * @return The position of the binary array in the {@link java.io.InputStream InputStream}
   *         corresponding to the MzML format data
   */
  public long getPosition() {
    return position;
  }

  /**
   * <p>
   * Set the position of the binary array in the {@link java.io.InputStream InputStream}
   * corresponding to the MzML format data
   * </p>
   *
   * @param position a <code>long</code> number defining the position of the binary array in the
   *        {@link java.io.InputStream InputStream} corresponding to the MzML format data
   */
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
}
