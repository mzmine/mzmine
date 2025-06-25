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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/**
 * Enumeration of different types of precision bit lengths which are parsed by the MzML Parser
 */
public enum MzMLBitLength {
  THIRTY_TWO_BIT_INTEGER("MS:1000519"), // 32-bit integer
  SIXTEEN_BIT_FLOAT("MS:1000520"), // 16-bit float
  THIRTY_TWO_BIT_FLOAT("MS:1000521"), // 32-bit float
  SIXTY_FOUR_BIT_INTEGER("MS:1000522"), // 64-bit integer
  SIXTY_FOUR_BIT_FLOAT("MS:1000523"); // 64-bit float

  private final String accession;

  private static final Map<String, MzMLBitLength> map = Arrays.stream(MzMLBitLength.values()).collect(
      Collectors.toMap(MzMLBitLength::getAccession, v -> v));

  MzMLBitLength(String accession) {
    this.accession = accession;
  }

  @Nullable
  public static MzMLBitLength of(String accession) {
    return map.get(accession);
  }

  /**
   * <p>getValue.</p>
   *
   * @return the CV Parameter accession of the precision bit length as {@link String String}
   */
  public String getValue() {
    return accession;
  }

  /**
   * returns accession see {@link #getValue()}
   */
  public String getAccession() {
    return accession;
  }

  public int bits() {
    return switch (this) {
      case SIXTY_FOUR_BIT_INTEGER, SIXTY_FOUR_BIT_FLOAT -> 64;
      case THIRTY_TWO_BIT_INTEGER, THIRTY_TWO_BIT_FLOAT -> 32;
      case SIXTEEN_BIT_FLOAT -> 16;
    };
  }
}
