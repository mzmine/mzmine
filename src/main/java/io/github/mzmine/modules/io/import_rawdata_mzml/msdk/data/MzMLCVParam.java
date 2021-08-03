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
