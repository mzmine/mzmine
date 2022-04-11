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

/**
 * Enumeration of different types of arrays which are parsed by the MzML Parser
 */
public enum MzMLArrayType {
  MZ("MS:1000514"), // m/z values array
  INTENSITY("MS:1000515"), // Intensity values array
  TIME("MS:1000595"); // Retention time values array

  private final String accession;

  MzMLArrayType(String accession) {
    this.accession = accession;
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
