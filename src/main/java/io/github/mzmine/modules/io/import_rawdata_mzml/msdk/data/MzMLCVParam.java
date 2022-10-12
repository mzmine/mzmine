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

import java.util.Optional;

/**
 * A CV Parameter object which contains the CV Parameter's accession and value, name & unitAccession
 * if available
 */
public class MzMLCVParam {

  private final String accession;
  private final Optional<String> value;
  private final Optional<String> name;
  private final Optional<String> unitAccession;

  /**
   * <p>
   * Constructor for MzMLCVParam
   * </p>
   *
   * @param accession the CV Parameter accession as {@link String String}
   * @param value the CV Parameter value as {@link String String}
   * @param name the CV Parameter name as {@link String String}
   * @param unitAccession the CV Parameter unit accession as {@link String String}
   */
  public MzMLCVParam(String accession, String value, String name, String unitAccession) {
    if (accession == null)
      throw new IllegalArgumentException("Accession can't be null");
    if (accession.length() == 0)
      throw new IllegalArgumentException("Accession can't be an empty string");
    this.accession = accession;
    if (value != null && value.length() == 0)
      value = null;
    this.value = Optional.ofNullable(value);
    if (name != null && name.length() == 0)
      name = null;
    this.name = Optional.ofNullable(name);
    if (unitAccession != null && unitAccession.length() == 0)
      unitAccession = null;
    this.unitAccession = Optional.ofNullable(unitAccession);
  }

  /**
   * <p>Getter for the field <code>accession</code>.</p>
   *
   * @return the CV Parameter accession as {@link String String}
   */
  public String getAccession() {
    return accession;
  }


  /**
   * <p>Getter for the field <code>value</code>.</p>
   *
   * @return the CV Parameter value as {@link Optional Optional<String>}
   */
  public Optional<String> getValue() {
    return value;
  }

  /**
   * <p>Getter for the field <code>name</code>.</p>
   *
   * @return the CV Parameter name as {@link Optional Optional<String>}
   */
  public Optional<String> getName() {
    return name;
  }

  /**
   * <p>Getter for the field <code>unitAccession</code>.</p>
   *
   * @return the CV Parameter unit accession as {@link Optional Optional<String>}
   */
  public Optional<String> getUnitAccession() {
    return unitAccession;
  }

}
