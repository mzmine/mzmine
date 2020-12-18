/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A frame is a collection of mobility resolved spectra at one point in time.
 */
public interface Frame extends Scan {

  /**
   * Equivalent to {@link Scan#getScanNumber()}.
   * @return the scan number
   */
  @Deprecated
  public default int getFrameId() {
    return getScanNumber();
  }

  public int getNumberOfMobilityScans();

  @Nonnull
  public MobilityType getMobilityType();

  /**
   *
   * @return Unsorted set of sub spectrum numbers.
   */
  public Set<Integer> getMobilityScanNumbers();

  @Nonnull
  public Range<Double> getMobilityRange();

  /**
   *
   * @param num the number of the sub spectrum
   * @return the sub spectrum
   */
  @Nullable
  public MobilityMassSpectrum getMobilityScan(int num);

  @Nonnull
  public List<MobilityMassSpectrum> getMobilityScans();

  public double getMobilityForSubSpectrum(int subSpectrumIndex);

  public Map<Integer, Double> getMobilities();

  public ImmutableList<Mobilogram> getMobilograms();

  public int addMobilogram(Mobilogram mobilogram);

  public void clearMobilograms();
}
