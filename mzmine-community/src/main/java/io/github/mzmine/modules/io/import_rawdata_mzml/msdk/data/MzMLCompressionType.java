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

/**
 * Enumeration of different compression types which are parsed by the MzML Parser
 */
public enum MzMLCompressionType {
  NUMPRESS_LINPRED("MS:1002312", "MS-Numpress linear prediction compression"), //
  NUMPRESS_POSINT("MS:1002313", "MS-Numpress positive integer compression"), //
  NUMPRESS_SHLOGF("MS:1002314", "MS-Numpress short logged float compression"), //
  ZLIB("MS:1000574", "zlib compression"), //
  NO_COMPRESSION("MS:1000576", "no compression"), //
  NUMPRESS_LINPRED_ZLIB("MS:1002746",
      "MS-Numpress linear prediction compression followed by zlib compression"), //
  NUMPRESS_POSINT_ZLIB("MS:1002747",
      "MS-Numpress positive integer compression followed by zlib compression"), //
  NUMPRESS_SHLOGF_ZLIB("MS:1002748",
      "MS-Numpress short logged float compression followed by zlib compression");

  private static final Map<String, MzMLCompressionType> map = Arrays.stream(MzMLCompressionType.values()).collect(
      Collectors.toMap(MzMLCompressionType::getAccession, v -> v));

  private String accession;
  private String name;

  MzMLCompressionType(String accession, String name) {
    this.accession = accession;
    this.name = name;
  }

  public static MzMLCompressionType ofAccession(String accession) {
    return map.get(accession);
  }

  public static boolean isCompressionTypeAccession(String accession) {
    return map.containsKey(accession);
  }

  /**
   * <p>Getter for the field <code>accession</code>.</p>
   *
   * @return the CV Parameter accession of the compression type as {@link String String}
   */
  public String getAccession() {
    return accession;
  }

  /**
   * <p>Getter for the field <code>name</code>.</p>
   *
   * @return the CV Parameter name of the compression type as {@link String String}
   */
  public String getName() {
    return name;
  }

  public boolean isZlibCompressed() {
    return switch (this) {
      case NUMPRESS_LINPRED, NO_COMPRESSION, NUMPRESS_SHLOGF, NUMPRESS_POSINT -> false;
      case ZLIB, NUMPRESS_LINPRED_ZLIB, NUMPRESS_POSINT_ZLIB, NUMPRESS_SHLOGF_ZLIB -> true;
    };
  }

  public boolean isNumpress() {
    return switch (this) {
      case NO_COMPRESSION, ZLIB -> false;
      case NUMPRESS_LINPRED, NUMPRESS_SHLOGF, NUMPRESS_POSINT, NUMPRESS_LINPRED_ZLIB, NUMPRESS_POSINT_ZLIB, NUMPRESS_SHLOGF_ZLIB ->
          true;
    };
  }
}


