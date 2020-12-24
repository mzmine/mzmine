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

import com.google.common.collect.Range;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A frame is a collection of mobility resolved spectra at one point in time.
 *
 * @author https://github.com/SteffenHeu
 */
public interface Frame extends Scan {

  /**
   * Equivalent to {@link Scan#getScanNumber()}.
   *
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
   * @return Unsorted set of sub spectrum numbers.
   */
  public Set<Integer> getMobilityScanNumbers();

  @Nonnull
  public Range<Double> getMobilityRange();

  /**
   * @param num the number of the sub scan.
   * @return the mobility scan or null of no scan with that number exists.
   */
  @Nullable
  public MobilityScan getMobilityScan(int num);

  @Nonnull
  public Collection<MobilityScan> getMobilityScans();

  /**
   * @param mobilityScanIndex
   * @return The mobility of this sub spectrum.
   */
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex);

  /**
   * @return Mapping of sub scan number <-> mobility
   */
  @Nullable
  public Map<Integer, Double> getMobilities();

  /**
   * @return Set of ImsMsMsInfos for this frame. Empty set if this is not an MS/MS frame or no
   * precursors were fragmented or assigned.
   */
  @Nonnull
  public Set<ImsMsMsInfo> getImsMsMsInfos();

  /**
   * @param mobilityScanNumber The sub scan number of the given sub scan.
   * @return ImsMsMsInfo or null if no precursor was fragmented at that scan.
   */
  @Nullable
  public ImsMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber);

  /**
   * @return Always 0.0
   */
  @Override
  default double getPrecursorMZ() {
    return 0.0d;
  }

  /**
   * @return Always 0
   */
  @Override
  default int getPrecursorCharge() {
    return 0;
  }
}
