/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

/**
 * Precursor information stored in IMS MS2 frames regarding their respective sub spectra.
 *
 * @author https://github.com/SteffenHeu
 */
public interface PasefMsMsInfo extends DDAMsMsInfo {

  String XML_SPECTRUM_NUMBER_RANGE_ATTR = "spectrumnumberrange";

  public final int UNKNOWN_CHARGE = 0;
  public final double UNKNOWN_COLISSIONENERGY = -1d;

  public Frame getMsMsFrame();

  public Frame getParentFrame();

  /**
   * @return The most intense m/z of the detected precursor.
   */
  @Nullable double getIsolationMz();

  /**
   * @return The range of spectra numbers in this frame where this precursor was fragmented in.
   */
  Range<Integer> getSpectrumNumberRange();

  /**
   * @return Collision energy this precursor was fragmented at in the given range. May be null if
   * not set or 0 if unknown.
   */
  Float getActivationEnergy();

  /**
   * @return The charge of the precursor. 0 = unknown.
   */
  Integer getPrecursorCharge();

  void writeToXML(XMLStreamWriter writer) throws XMLStreamException;
}
