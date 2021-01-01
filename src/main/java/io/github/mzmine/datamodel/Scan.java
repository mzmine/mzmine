/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General License as published by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * License for more details.
 *
 * You should have received a copy of the GNU General License along with MZmine; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;

/**
 * This class represent one spectrum of a raw data file.
 */
public interface Scan extends MassSpectrum {

  /**
   *
   * @return RawDataFile containing this Scan
   */
  @Nonnull
  RawDataFile getDataFile();

  /**
   *
   * @return Scan number
   */
  int getScanNumber();

  /**
   *
   * @return Instrument-specific scan definition as String
   */
  String getScanDefinition();

  /**
   *
   * @return MS level
   */
  int getMSLevel();

  /**
   *
   * @return Retention time of this scan in minutes
   */
  float getRetentionTime();

  /**
   *
   * @return The actual scanning range of the instrument
   */
  @Nonnull
  Range<Double> getScanningMZRange();

  /**
   *
   * @return Precursor m/z or 0 if this is not MSn scan
   */
  double getPrecursorMZ();

  @Nonnull
  PolarityType getPolarity();

  /**
   *
   * @return Precursor charge or 0 if this is not MSn scan or charge is unknown
   */
  int getPrecursorCharge();

  @Nonnull
  MassList[] getMassLists();

  @Nullable
  MassList getMassList(@Nonnull String name);

  void addMassList(@Nonnull MassList massList);

  void removeMassList(@Nonnull MassList massList);

  /**
   * @see io.github.mzmine.datamodel.Scan#getSpectrumType()
   */
  @Override
  MassSpectrumType getSpectrumType();

  void setSpectrumType(MassSpectrumType spectrumType);

}

