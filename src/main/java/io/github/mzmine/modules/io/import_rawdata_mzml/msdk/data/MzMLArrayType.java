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
