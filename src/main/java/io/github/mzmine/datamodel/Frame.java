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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;

/**
 * A frame is a collection of mobility resolved spectra at one point in time.
 */
public interface Frame extends Scan {

  /**
   * Equivalent to {@link Scan#getScanNumber()}.
   *
   * @return the scan number
   */
  @Deprecated
  default int getFrameId() {
    return getScanNumber();
  }

  int getNumberOfMobilityScans();

  @Nonnull
  MobilityType getMobilityType();

  /**
   *
   * @return Unsorted set of sub spectrum numbers.
   */
  Set<Integer> getMobilityScanNumbers();

  @Nonnull
  Range<Double> getMobilityRange();

  /**
   * @param num the number of the sub scan.
   * @return the mobility scan or null of no scan with that number exists.
   */
  @Nullable
  MobilityScan getMobilityScan(int num);

  @Nonnull
  Collection<MobilityScan> getMobilityScans();

  /**
   * @param mobilityScanIndex
   * @return The mobility of this sub spectrum.
   */
  double getMobilityForMobilityScanNumber(int mobilityScanIndex);

  /**
   * @return Mapping of sub scan number <-> mobility
   */
  @Nullable
  Map<Integer, Double> getMobilities();

  /**
   * @return Set of ImsMsMsInfos for this frame. Empty set if this is not an MS/MS frame or no
   *         precursors were fragmented or assigned.
   */
  @Nonnull
  Set<ImsMsMsInfo> getImsMsMsInfos();

  /**
   * @param mobilityScanNumber The sub scan number of the given sub scan.
   * @return ImsMsMsInfo or null if no precursor was fragmented at that scan.
   */
  @Nullable
  ImsMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber);

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

  void addMobilityScan(MobilityScan originalMobilityScan);

  // ImmutableList<Mobilogram> getMobilograms();

  // int addMobilogram(Mobilogram mobilogram);

  // void clearMobilograms();
}
