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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represent one spectrum of a raw data file.
 */
public interface Scan extends MassSpectrum, Comparable<Scan> {

  /**
   * @return RawDataFile containing this Scan
   */
  @NotNull
  RawDataFile getDataFile();

  /**
   * @return Scan number
   */
  int getScanNumber();

  /**
   * @return Instrument-specific scan definition as String
   */
  @NotNull
  String getScanDefinition();

  /**
   * @return MS level
   */
  int getMSLevel();

  /**
   * @return Retention time of this scan in minutes
   */
  float getRetentionTime();

  /**
   * @return The actual scanning range of the instrument
   */
  @NotNull
  Range<Double> getScanningMZRange();

  /**
   * @return Precursor m/z or 0 if this is not MSn scan
   */
  double getPrecursorMZ();

  @NotNull
  PolarityType getPolarity();

  /**
   * @return Precursor charge, 0 if this is not MSn scan, -1 if charge is unknown
   */
  int getPrecursorCharge();

  @Nullable
  MassList getMassList();

  void addMassList(@NotNull MassList massList);

  /**
   * Standard method to sort scans based on scan number (or if not available retention time)
   *
   * @param s other scan
   * @return
   */
  @Override
  default int compareTo(@NotNull Scan s) {
    assert s != null;
    int result = Integer.compare(this.getScanNumber(), s.getScanNumber());
    if (result != 0) {
      return result;
    } else {
      return Float.compare(this.getRetentionTime(), s.getRetentionTime());
    }
  }
}

