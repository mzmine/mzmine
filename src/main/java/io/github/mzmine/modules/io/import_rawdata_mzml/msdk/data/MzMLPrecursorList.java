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

import java.util.ArrayList;


/**
 * <p>MzMLPrecursorList class.</p>
 *
 */
public class MzMLPrecursorList {

  private ArrayList<MzMLPrecursorElement> precursorElements;

  /**
   * <p>Constructor for MzMLPrecursorList.</p>
   */
  public MzMLPrecursorList() {
    this.precursorElements = new ArrayList<>();
  }

  /**
   * <p>Getter for the field <code>precursorElements</code>.</p>
   *
   * @return a {@link ArrayList} object.
   */
  public ArrayList<MzMLPrecursorElement> getPrecursorElements() {
    return precursorElements;
  }

  /**
   * <p>addPrecursor.</p>
   *
   * @param precursor a {@link MzMLPrecursorElement} object.
   */
  public void addPrecursor(MzMLPrecursorElement precursor) {
    precursorElements.add(precursor);
  }
}
