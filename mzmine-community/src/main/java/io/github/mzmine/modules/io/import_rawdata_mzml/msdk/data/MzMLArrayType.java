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
 * Enumeration of different types of arrays which are parsed by the MzML Parser
 */
public enum MzMLArrayType {
  MZ("MS:1000514"), // m/z values array
  INTENSITY("MS:1000515"), // Intensity values array
  TIME("MS:1000595"), // Retention time values array
  WAVELENGTH("MS:1000617"); // wavelength array, eg. PDA detector

  private static final Map<String, MzMLArrayType> map = Arrays.stream(MzMLArrayType.values()).collect(
      Collectors.toMap(MzMLArrayType::getAccession, v -> v));

  private final String accession;

  MzMLArrayType(String accession) {
    this.accession = accession;
  }

  public static MzMLArrayType ofAccession(String accession) {
    return map.get(accession);
  }

  public static boolean isArrayTypeAccession(String accession) {
    return map.containsKey(accession);
  }

  /**
   * <p>Getter for the field <code>accession</code>.</p>
   *
   * @return the CV Parameter accession of the binary data array type as {@link String
   *         String}
   */
  public String getAccession() {
    return accession;
  }
}
